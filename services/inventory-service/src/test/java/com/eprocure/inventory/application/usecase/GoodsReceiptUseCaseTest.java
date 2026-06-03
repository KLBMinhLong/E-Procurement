package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.inventory.application.port.in.CompleteGoodsReceiptCommand;
import com.eprocure.inventory.application.port.in.CreateGoodsReceiptCommand;
import com.eprocure.inventory.application.port.in.GetGoodsReceiptQuery;
import com.eprocure.inventory.application.port.in.ListGoodsReceiptsQuery;
import com.eprocure.inventory.application.port.out.GrCreatedEventPublisher;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.domain.event.GrCreatedEvent;
import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.model.StockMovementType;
import com.eprocure.inventory.domain.repository.GoodsReceiptFilter;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import com.eprocure.inventory.domain.repository.PurchaseOrderSnapshotRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoodsReceiptUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-03T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID PO_ID = UUID.fromString("96000000-0000-4000-8000-000000000001");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("92000000-0000-4000-8000-000000000001");
    private static final UUID WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000001");
    private static final UUID PO_LINE_ITEM_ID = UUID.fromString("97000000-0000-4000-8000-000000000001");
    private static final UUID PR_LINE_ITEM_ID = UUID.fromString("95000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final String COMPLETE_IDEMPOTENCY_KEY = "22222222-2222-4222-8222-222222222222";
    private static final String OTHER_COMPLETE_IDEMPOTENCY_KEY = "33333333-3333-4333-8333-333333333333";

    private FakeGoodsReceiptRepository goodsReceiptRepository;
    private FakePurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository;
    private FakeGrCreatedEventPublisher grCreatedEventPublisher;

    @BeforeEach
    void setUp() {
        goodsReceiptRepository = new FakeGoodsReceiptRepository();
        purchaseOrderSnapshotRepository = new FakePurchaseOrderSnapshotRepository();
        purchaseOrderSnapshotRepository.snapshots.add(poSnapshot());
        grCreatedEventPublisher = new FakeGrCreatedEventPublisher();
    }

    @Test
    void should_create_draft_goods_receipt_when_po_snapshot_exists() {
        var useCase = createUseCase();

        var result = useCase.execute(command(new BigDecimal("9.0000")), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.view().grNumber()).isEqualTo("GR-2026-000001");
        assertThat(result.view().status()).isEqualTo(GoodsReceiptStatus.DRAFT);
        assertThat(result.view().po().id()).isEqualTo(PO_ID);
        assertThat(result.view().warehouse().id()).isEqualTo(WAREHOUSE_ID);
        assertThat(result.view().lineItems()).hasSize(1);
        assertThat(result.view().lineItems().get(0).itemCode()).isNull();
        assertThat(result.view().lineItems().get(0).orderedQuantity()).isEqualByComparingTo("10.0000");
        assertThat(goodsReceiptRepository.goodsReceipts).hasSize(1);
    }

    @Test
    void should_replay_existing_goods_receipt_when_idempotency_key_reused() {
        var useCase = createUseCase();
        useCase.execute(command(new BigDecimal("9.0000")), IDEMPOTENCY_KEY);

        var replayed = useCase.execute(command(new BigDecimal("9.0000")), IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(goodsReceiptRepository.goodsReceipts).hasSize(1);
        assertThat(goodsReceiptRepository.generatedGrNumbers).isEqualTo(1);
    }

    @Test
    void should_throw_inv009_when_po_snapshot_missing() {
        purchaseOrderSnapshotRepository.snapshots.clear();
        var useCase = createUseCase();

        assertThatThrownBy(() -> useCase.execute(command(new BigDecimal("9.0000")), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_009);
    }

    @Test
    void should_throw_inv006_when_received_quantity_exceeds_tolerance() {
        var useCase = createUseCase();

        assertThatThrownBy(() -> useCase.execute(command(new BigDecimal("12.0000")), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_006);
    }

    @Test
    void should_get_goods_receipt_when_existing() {
        createUseCase().execute(command(new BigDecimal("9.0000")), IDEMPOTENCY_KEY);
        GoodsReceipt goodsReceipt = goodsReceiptRepository.goodsReceipts.get(0);
        var useCase = new GetGoodsReceiptUseCase(goodsReceiptRepository);

        var result = useCase.execute(new GetGoodsReceiptQuery(ACTOR_ID, goodsReceipt.id()));

        assertThat(result.id()).isEqualTo(goodsReceipt.id());
        assertThat(result.lineItems()).hasSize(1);
    }

    @Test
    void should_list_goods_receipts_when_filter_matches() {
        createUseCase().execute(command(new BigDecimal("9.0000")), IDEMPOTENCY_KEY);
        var useCase = new ListGoodsReceiptsUseCase(goodsReceiptRepository);

        var result = useCase.execute(new ListGoodsReceiptsQuery(
                ACTOR_ID,
                GoodsReceiptStatus.DRAFT,
                PO_ID,
                WAREHOUSE_ID,
                LocalDate.parse("2026-06-03"),
                LocalDate.parse("2026-06-03"),
                1,
                20));

        assertThat(result.items()).hasSize(1);
        assertThat(result.meta().totalElements()).isEqualTo(1);
    }

    @Test
    void should_complete_goods_receipt_and_create_receipt_in_movement_when_draft() {
        createUseCase().execute(command(new BigDecimal("10.0000")), IDEMPOTENCY_KEY);
        GoodsReceipt goodsReceipt = goodsReceiptRepository.goodsReceipts.get(0);
        var useCase = completeUseCase();

        var result = useCase.execute(
                new CompleteGoodsReceiptCommand(ACTOR_ID, goodsReceipt.id()),
                COMPLETE_IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.grStatus()).isEqualTo(GoodsReceiptStatus.COMPLETE);
        assertThat(result.movementsCreated()).isEqualTo(1);
        assertThat(result.updatedStocks()).singleElement().satisfies(stock -> {
            assertThat(stock.itemCode()).isEqualTo("IT-LAPTOP-001");
            assertThat(stock.newQuantityOnHand()).isEqualByComparingTo("10.0000");
        });
        assertThat(goodsReceiptRepository.stockMovements).singleElement().satisfies(movement -> {
            assertThat(movement.movementType()).isEqualTo(StockMovementType.RECEIPT_IN);
            assertThat(movement.quantity()).isEqualByComparingTo("10.0000");
            assertThat(movement.sourceRefId()).isEqualTo(goodsReceipt.id());
        });
        assertThat(goodsReceiptRepository.findById(goodsReceipt.id()).orElseThrow().status())
                .isEqualTo(GoodsReceiptStatus.COMPLETE);
        assertThat(goodsReceiptRepository.findById(goodsReceipt.id()).orElseThrow().lineItems().get(0).itemCode())
                .isEqualTo("IT-LAPTOP-001");
        assertThat(grCreatedEventPublisher.events).hasSize(1);
        assertThat(grCreatedEventPublisher.events.get(0).payload().status()).isEqualTo(GoodsReceiptStatus.COMPLETE);
    }

    @Test
    void should_replay_complete_goods_receipt_when_complete_idempotency_key_reused() {
        createUseCase().execute(command(new BigDecimal("10.0000")), IDEMPOTENCY_KEY);
        GoodsReceipt goodsReceipt = goodsReceiptRepository.goodsReceipts.get(0);
        var useCase = completeUseCase();
        useCase.execute(new CompleteGoodsReceiptCommand(ACTOR_ID, goodsReceipt.id()), COMPLETE_IDEMPOTENCY_KEY);

        var replayed = useCase.execute(new CompleteGoodsReceiptCommand(ACTOR_ID, goodsReceipt.id()), COMPLETE_IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.movementsCreated()).isEqualTo(1);
        assertThat(goodsReceiptRepository.stockMovements).hasSize(1);
        assertThat(grCreatedEventPublisher.events).hasSize(1);
    }

    @Test
    void should_throw_inv005_when_goods_receipt_already_completed_with_different_key() {
        createUseCase().execute(command(new BigDecimal("10.0000")), IDEMPOTENCY_KEY);
        GoodsReceipt goodsReceipt = goodsReceiptRepository.goodsReceipts.get(0);
        var useCase = completeUseCase();
        useCase.execute(new CompleteGoodsReceiptCommand(ACTOR_ID, goodsReceipt.id()), COMPLETE_IDEMPOTENCY_KEY);

        assertThatThrownBy(() -> useCase.execute(
                new CompleteGoodsReceiptCommand(ACTOR_ID, goodsReceipt.id()),
                OTHER_COMPLETE_IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_005);
    }

    @Test
    void should_throw_inv001_when_catalog_item_cannot_be_resolved_for_completion() {
        goodsReceiptRepository.itemCodesByPoLineItem.clear();
        createUseCase().execute(command(new BigDecimal("10.0000")), IDEMPOTENCY_KEY);
        GoodsReceipt goodsReceipt = goodsReceiptRepository.goodsReceipts.get(0);
        var useCase = completeUseCase();

        assertThatThrownBy(() -> useCase.execute(
                new CompleteGoodsReceiptCommand(ACTOR_ID, goodsReceipt.id()),
                COMPLETE_IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_001);
        assertThat(goodsReceiptRepository.stockMovements).isEmpty();
        assertThat(goodsReceiptRepository.findById(goodsReceipt.id()).orElseThrow().status())
                .isEqualTo(GoodsReceiptStatus.DRAFT);
    }

    private CreateGoodsReceiptUseCase createUseCase() {
        return new CreateGoodsReceiptUseCase(
                goodsReceiptRepository,
                purchaseOrderSnapshotRepository,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new BigDecimal("10"));
    }

    private CompleteGoodsReceiptUseCase completeUseCase() {
        return new CompleteGoodsReceiptUseCase(
                goodsReceiptRepository,
                grCreatedEventPublisher,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CreateGoodsReceiptCommand command(BigDecimal receivedQuantity) {
        return new CreateGoodsReceiptCommand(
                ACTOR_ID,
                "Warehouse Keeper",
                PO_ID,
                WAREHOUSE_ID,
                NOW,
                List.of(new CreateGoodsReceiptCommand.LineItem(
                        PO_LINE_ITEM_ID,
                        receivedQuantity,
                        BigDecimal.ZERO,
                        null,
                        "LOT-001")),
                "Received at dock A");
    }

    private static PurchaseOrderSnapshot poSnapshot() {
        return new PurchaseOrderSnapshot(
                UUID.randomUUID(),
                PO_ID,
                "PO-2026-000001",
                PR_ID,
                "PR-2026-000001",
                VENDOR_ID,
                "Acme Supplier",
                "sales@acme.example",
                "ACME-TAX",
                ACTOR_ID,
                new BigDecimal("25000000.0000"),
                "VND",
                "Floor 10, eProcure Tower",
                LocalDate.parse("2026-06-30"),
                "NET30",
                NOW,
                NOW,
                NOW,
                "evt-po-issued-001",
                List.of(new PurchaseOrderSnapshot.LineItem(
                        PO_LINE_ITEM_ID,
                        PR_LINE_ITEM_ID,
                        "Laptop",
                        "IT",
                        new BigDecimal("10.0000"),
                        "PCS",
                        new BigDecimal("2500000.0000"),
                        new BigDecimal("25000000.0000"),
                        "VND")));
    }

    private static final class FakeGoodsReceiptRepository implements GoodsReceiptRepository {
        private final List<GoodsReceipt> goodsReceipts = new ArrayList<>();
        private final List<StockMovement> stockMovements = new ArrayList<>();
        private final Map<UUID, UUID> completeIdempotencyKeys = new HashMap<>();
        private final Map<UUID, String> itemCodesByPoLineItem = new HashMap<>(Map.of(PO_LINE_ITEM_ID, "IT-LAPTOP-001"));
        private final Map<String, BigDecimal> stockBalances = new LinkedHashMap<>();
        private int generatedGrNumbers;

        @Override
        public Optional<GoodsReceipt> findById(UUID id) {
            return goodsReceipts.stream()
                    .filter(goodsReceipt -> goodsReceipt.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<GoodsReceipt> findByIdempotencyKey(UUID idempotencyKey) {
            return goodsReceipts.stream()
                    .filter(goodsReceipt -> goodsReceipt.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public Optional<GoodsReceipt> findByIdAndCompleteIdempotencyKey(UUID id, UUID idempotencyKey) {
            return goodsReceipts.stream()
                    .filter(goodsReceipt -> goodsReceipt.id().equals(id))
                    .filter(goodsReceipt -> idempotencyKey.equals(completeIdempotencyKeys.get(goodsReceipt.id())))
                    .findFirst();
        }

        @Override
        public List<GoodsReceipt> findByFilter(GoodsReceiptFilter filter) {
            return goodsReceipts.stream()
                    .filter(goodsReceipt -> filter.status() == null || goodsReceipt.status() == filter.status())
                    .filter(goodsReceipt -> filter.poId() == null || goodsReceipt.poId().equals(filter.poId()))
                    .filter(goodsReceipt -> filter.warehouseId() == null || goodsReceipt.warehouseId().equals(filter.warehouseId()))
                    .filter(goodsReceipt -> filter.fromReceivedAt() == null || !goodsReceipt.receivedAt().isBefore(filter.fromReceivedAt()))
                    .filter(goodsReceipt -> filter.toReceivedAtExclusive() == null || goodsReceipt.receivedAt().isBefore(filter.toReceivedAtExclusive()))
                    .sorted(Comparator.comparing(GoodsReceipt::createdAt).reversed())
                    .skip(filter.offset())
                    .limit(filter.size())
                    .toList();
        }

        @Override
        public long countByFilter(GoodsReceiptFilter filter) {
            return findByFilter(new GoodsReceiptFilter(
                    filter.status(),
                    filter.poId(),
                    filter.warehouseId(),
                    filter.fromReceivedAt(),
                    filter.toReceivedAtExclusive(),
                    1,
                    Integer.MAX_VALUE,
                    0)).size();
        }

        @Override
        public Optional<WarehouseSnapshot> findActiveWarehouseById(UUID warehouseId) {
            return WAREHOUSE_ID.equals(warehouseId)
                    ? Optional.of(new WarehouseSnapshot(WAREHOUSE_ID, "Main Warehouse"))
                    : Optional.empty();
        }

        @Override
        public String nextGrNumber(int fiscalYear) {
            generatedGrNumbers++;
            return "GR-" + fiscalYear + "-" + String.format("%06d", generatedGrNumbers);
        }

        @Override
        public void insert(GoodsReceipt goodsReceipt) {
            goodsReceipts.add(goodsReceipt);
        }

        @Override
        public Optional<String> findActiveItemCodeForPoLineItem(UUID poLineItemId) {
            return Optional.ofNullable(itemCodesByPoLineItem.get(poLineItemId));
        }

        @Override
        public void updateLineItemCode(UUID lineItemId, String itemCode, UUID actorId) {
            replaceGoodsReceipt(goodsReceipts.stream()
                    .filter(goodsReceipt -> goodsReceipt.lineItems().stream()
                            .anyMatch(lineItem -> lineItem.id().equals(lineItemId)))
                    .findFirst()
                    .map(goodsReceipt -> new GoodsReceipt(
                            goodsReceipt.id(),
                            goodsReceipt.grNumber(),
                            goodsReceipt.poId(),
                            goodsReceipt.poNumber(),
                            goodsReceipt.warehouseId(),
                            goodsReceipt.warehouseName(),
                            goodsReceipt.warehouseKeeperId(),
                            goodsReceipt.warehouseKeeperFullName(),
                            goodsReceipt.receivedAt(),
                            goodsReceipt.status(),
                            goodsReceipt.lineItems().stream()
                                    .map(lineItem -> lineItem.id().equals(lineItemId)
                                            ? new com.eprocure.inventory.domain.model.GoodsReceiptLineItem(
                                                    lineItem.id(),
                                                    lineItem.poLineItemId(),
                                                    itemCode,
                                                    lineItem.itemName(),
                                                    lineItem.orderedQuantity(),
                                                    lineItem.receivedQuantity(),
                                                    lineItem.rejectedQuantity(),
                                                    lineItem.unit(),
                                                    lineItem.rejectionReason(),
                                                    lineItem.lotNumber())
                                            : lineItem)
                                    .toList(),
                            goodsReceipt.notes(),
                            goodsReceipt.createdAt(),
                            goodsReceipt.createdBy(),
                            actorId,
                            goodsReceipt.idempotencyKey()))
                    .orElseThrow());
        }

        @Override
        public BigDecimal receiveStock(String itemCode, UUID warehouseId, BigDecimal quantity, String unit, UUID actorId, Instant occurredAt) {
            String key = itemCode + "|" + warehouseId;
            BigDecimal balance = stockBalances.getOrDefault(key, BigDecimal.ZERO).add(quantity);
            stockBalances.put(key, balance);
            return balance;
        }

        @Override
        public void insertStockMovement(StockMovement stockMovement) {
            stockMovements.add(stockMovement);
        }

        @Override
        public boolean markCompleted(UUID id, GoodsReceiptStatus status, UUID actorId, Instant completedAt, UUID idempotencyKey) {
            GoodsReceipt goodsReceipt = findById(id).orElseThrow();
            if (goodsReceipt.status() != GoodsReceiptStatus.DRAFT) {
                return false;
            }
            replaceGoodsReceipt(new GoodsReceipt(
                    goodsReceipt.id(),
                    goodsReceipt.grNumber(),
                    goodsReceipt.poId(),
                    goodsReceipt.poNumber(),
                    goodsReceipt.warehouseId(),
                    goodsReceipt.warehouseName(),
                    goodsReceipt.warehouseKeeperId(),
                    goodsReceipt.warehouseKeeperFullName(),
                    goodsReceipt.receivedAt(),
                    status,
                    goodsReceipt.lineItems(),
                    goodsReceipt.notes(),
                    goodsReceipt.createdAt(),
                    goodsReceipt.createdBy(),
                    actorId,
                    goodsReceipt.idempotencyKey()));
            completeIdempotencyKeys.put(id, idempotencyKey);
            return true;
        }

        @Override
        public int countReceiptMovements(UUID goodsReceiptId) {
            return (int) stockMovements.stream()
                    .filter(movement -> movement.sourceRefId().equals(goodsReceiptId))
                    .count();
        }

        @Override
        public List<StockBalance> findStockBalancesByReceipt(UUID goodsReceiptId) {
            return stockMovements.stream()
                    .filter(movement -> movement.sourceRefId().equals(goodsReceiptId))
                    .map(movement -> new StockBalance(
                            movement.itemCode(),
                            stockBalances.get(movement.itemCode() + "|" + movement.warehouseId())))
                    .distinct()
                    .toList();
        }

        private void replaceGoodsReceipt(GoodsReceipt replacement) {
            goodsReceipts.replaceAll(goodsReceipt -> goodsReceipt.id().equals(replacement.id()) ? replacement : goodsReceipt);
        }
    }

    private static final class FakeGrCreatedEventPublisher implements GrCreatedEventPublisher {
        private final List<GrCreatedEvent> events = new ArrayList<>();

        @Override
        public void publish(GrCreatedEvent event) {
            events.add(event);
        }
    }

    private static final class FakePurchaseOrderSnapshotRepository implements PurchaseOrderSnapshotRepository {
        private final List<PurchaseOrderSnapshot> snapshots = new ArrayList<>();
        private final Set<String> processedEvents = new HashSet<>();

        @Override
        public Optional<PurchaseOrderSnapshot> findByPoId(UUID poId) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.poId().equals(poId))
                    .findFirst();
        }

        @Override
        public Optional<PurchaseOrderSnapshot> findBySourceEventId(String sourceEventId) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.sourceEventId().equals(sourceEventId))
                    .findFirst();
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return processedEvents.contains(eventId);
        }

        @Override
        public void insert(PurchaseOrderSnapshot purchaseOrderSnapshot) {
            snapshots.add(purchaseOrderSnapshot);
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            processedEvents.add(eventId);
        }
    }
}
