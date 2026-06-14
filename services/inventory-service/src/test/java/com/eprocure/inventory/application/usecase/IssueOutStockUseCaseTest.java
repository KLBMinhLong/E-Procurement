package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.inventory.application.port.in.IssueOutStockCommand;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.domain.model.StockAdjustmentRequest;
import com.eprocure.inventory.domain.model.StockEntry;
import com.eprocure.inventory.domain.model.StockIssueOutRequest;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.model.StockMovementHistory;
import com.eprocure.inventory.domain.model.StockMovementType;
import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IssueOutStockUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-03T05:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000001");
    private static final UUID UNKNOWN_WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000099");
    private static final UUID RECIPIENT_ID = UUID.fromString("31000000-0000-4000-8000-000000000001");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "44444444-4444-4444-8444-444444444444";

    private FakeStockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository = new FakeStockRepository();
        stockRepository.itemCodes.add("IT-LAPTOP-001");
        stockRepository.itemNames.put("IT-LAPTOP-001", "Laptop");
        stockRepository.warehouseIds.add(WAREHOUSE_ID);
        stockRepository.stockBalances.put("IT-LAPTOP-001|" + WAREHOUSE_ID + "|PCS", new BigDecimal("10.0000"));
    }

    @Test
    void should_issue_out_stock_and_create_issue_out_movement_when_sufficient_stock() {
        var useCase = useCase();

        var result = useCase.execute(command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("3.0000")), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.movements()).singleElement().satisfies(movement -> {
            assertThat(movement.itemCode()).isEqualTo("IT-LAPTOP-001");
            assertThat(movement.movementType()).isEqualTo(StockMovementType.ISSUE_OUT);
            assertThat(movement.quantity()).isEqualByComparingTo("-3.0000");
            assertThat(movement.balanceAfter()).isEqualByComparingTo("7.0000");
            assertThat(movement.sourceRefType()).isEqualTo("STOCK_ISSUE_OUT");
        });
        assertThat(stockRepository.issueOutRequests).hasSize(1);
        assertThat(stockRepository.stockBalances.get("IT-LAPTOP-001|" + WAREHOUSE_ID + "|PCS"))
                .isEqualByComparingTo("7.0000");
    }

    @Test
    void should_replay_issue_out_when_idempotency_key_reused() {
        var useCase = useCase();
        useCase.execute(command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("3.0000")), IDEMPOTENCY_KEY);

        var replayed = useCase.execute(command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("3.0000")), IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.movements()).hasSize(1);
        assertThat(stockRepository.issueOutRequests).hasSize(1);
        assertThat(stockRepository.stockMovements).hasSize(1);
        assertThat(stockRepository.stockBalances.get("IT-LAPTOP-001|" + WAREHOUSE_ID + "|PCS"))
                .isEqualByComparingTo("7.0000");
    }

    @Test
    void should_throw_inv003_when_stock_is_insufficient() {
        var useCase = useCase();

        assertThatThrownBy(() -> useCase.execute(
                command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("11.0000")),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_003);
        assertThat(stockRepository.stockMovements).isEmpty();
    }

    @Test
    void should_throw_inv001_when_item_is_missing() {
        var useCase = useCase();

        assertThatThrownBy(() -> useCase.execute(
                command(WAREHOUSE_ID, "UNKNOWN", new BigDecimal("1.0000")),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_001);
        assertThat(stockRepository.issueOutRequests).isEmpty();
    }

    @Test
    void should_throw_inv002_when_warehouse_is_missing() {
        var useCase = useCase();

        assertThatThrownBy(() -> useCase.execute(
                command(UNKNOWN_WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("1.0000")),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_002);
        assertThat(stockRepository.issueOutRequests).isEmpty();
    }

    private IssueOutStockUseCase useCase() {
        return new IssueOutStockUseCase(
                stockRepository,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static IssueOutStockCommand command(UUID warehouseId, String itemCode, BigDecimal quantity) {
        return new IssueOutStockCommand(
                ACTOR_ID,
                warehouseId,
                PR_ID,
                RECIPIENT_ID,
                List.of(new IssueOutStockCommand.LineItem(itemCode, quantity, "PCS")),
                "Issue to requester");
    }

    private static final class FakeStockRepository implements StockRepository {
        private final Set<String> itemCodes = new HashSet<>();
        private final Map<String, String> itemNames = new LinkedHashMap<>();
        private final Set<UUID> warehouseIds = new HashSet<>();
        private final Map<String, BigDecimal> stockBalances = new LinkedHashMap<>();
        private final List<StockIssueOutRequest> issueOutRequests = new ArrayList<>();
        private final List<StockMovement> stockMovements = new ArrayList<>();

        @Override
        public boolean existsActiveItem(String itemCode) {
            return itemCodes.contains(itemCode);
        }

        @Override
        public boolean existsActiveWarehouse(UUID warehouseId) {
            return warehouseIds.contains(warehouseId);
        }

        @Override
        public List<StockEntry> findStockEntries(StockEntryFilter filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public long countStockEntries(StockEntryFilter filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public List<StockMovementHistory> findMovements(StockMovementFilter filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public long countMovements(StockMovementFilter filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public Optional<StockIssueOutRequest> findIssueOutRequestByIdempotencyKey(UUID idempotencyKey) {
            return issueOutRequests.stream()
                    .filter(request -> request.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public void insertIssueOutRequest(StockIssueOutRequest issueOutRequest) {
            issueOutRequests.add(issueOutRequest);
        }

        @Override
        public Optional<StockAdjustmentRequest> findAdjustmentRequestByIdempotencyKey(UUID idempotencyKey) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void insertAdjustmentRequest(StockAdjustmentRequest adjustmentRequest) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public Optional<BigDecimal> findStockQuantity(String itemCode, UUID warehouseId, String unit) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public Optional<BigDecimal> issueStock(
                String itemCode,
                UUID warehouseId,
                BigDecimal quantity,
                String unit,
                UUID actorId,
                Instant occurredAt) {
            String key = itemCode + "|" + warehouseId + "|" + unit;
            BigDecimal current = stockBalances.get(key);
            if (current == null || current.compareTo(quantity) < 0) {
                return Optional.empty();
            }
            BigDecimal balanceAfter = current.subtract(quantity);
            stockBalances.put(key, balanceAfter);
            return Optional.of(balanceAfter);
        }

        @Override
        public BigDecimal adjustStock(
                String itemCode,
                UUID warehouseId,
                BigDecimal newQuantity,
                String unit,
                UUID actorId,
                Instant occurredAt) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void insertStockMovement(StockMovement stockMovement) {
            stockMovements.add(stockMovement);
        }

        @Override
        public List<StockMovementHistory> findMovementsBySource(String sourceRefType, UUID sourceRefId) {
            return stockMovements.stream()
                    .filter(movement -> sourceRefType.equals(movement.sourceRefType()))
                    .filter(movement -> sourceRefId.equals(movement.sourceRefId()))
                    .map(this::toHistory)
                    .toList();
        }

        private StockMovementHistory toHistory(StockMovement movement) {
            return new StockMovementHistory(
                    movement.id(),
                    movement.itemCode(),
                    itemNames.getOrDefault(movement.itemCode(), movement.itemCode()),
                    movement.warehouseId(),
                    movement.movementType(),
                    movement.quantity(),
                    movement.unit(),
                    movement.balanceAfter(),
                    movement.sourceRefType(),
                    movement.sourceRefId(),
                    movement.performedBy(),
                    movement.performedBy().toString(),
                    movement.performedAt(),
                    movement.notes());
        }
    }
}
