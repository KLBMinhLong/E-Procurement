package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.inventory.application.port.in.CreateGoodsReceiptCommand;
import com.eprocure.inventory.application.port.in.GetGoodsReceiptQuery;
import com.eprocure.inventory.application.port.in.ListGoodsReceiptsQuery;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
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
import java.util.HashSet;
import java.util.List;
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

    private FakeGoodsReceiptRepository goodsReceiptRepository;
    private FakePurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository;

    @BeforeEach
    void setUp() {
        goodsReceiptRepository = new FakeGoodsReceiptRepository();
        purchaseOrderSnapshotRepository = new FakePurchaseOrderSnapshotRepository();
        purchaseOrderSnapshotRepository.snapshots.add(poSnapshot());
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

    private CreateGoodsReceiptUseCase createUseCase() {
        return new CreateGoodsReceiptUseCase(
                goodsReceiptRepository,
                purchaseOrderSnapshotRepository,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new BigDecimal("10"));
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
