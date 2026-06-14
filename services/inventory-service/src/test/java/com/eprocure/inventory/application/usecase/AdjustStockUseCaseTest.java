package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.inventory.application.port.in.AdjustStockCommand;
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

class AdjustStockUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-12T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000001");
    private static final UUID UNKNOWN_WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000099");
    private static final String IDEMPOTENCY_KEY = "55555555-5555-4555-8555-555555555555";

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
    void should_adjust_stock_and_create_adjustment_movement_when_new_quantity_differs() {
        var useCase = useCase();

        var result = useCase.execute(command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("7.5000")), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.movement()).satisfies(movement -> {
            assertThat(movement.itemCode()).isEqualTo("IT-LAPTOP-001");
            assertThat(movement.movementType()).isEqualTo(StockMovementType.ADJUSTMENT);
            assertThat(movement.quantity()).isEqualByComparingTo("-2.5000");
            assertThat(movement.balanceAfter()).isEqualByComparingTo("7.5000");
            assertThat(movement.sourceRefType()).isEqualTo("STOCK_ADJUSTMENT");
        });
        assertThat(stockRepository.adjustmentRequests).singleElement().satisfies(request -> {
            assertThat(request.previousQuantity()).isEqualByComparingTo("10.0000");
            assertThat(request.newQuantity()).isEqualByComparingTo("7.5000");
            assertThat(request.reason()).isEqualTo("Annual stock count correction");
        });
        assertThat(stockRepository.stockBalances.get("IT-LAPTOP-001|" + WAREHOUSE_ID + "|PCS"))
                .isEqualByComparingTo("7.5000");
    }

    @Test
    void should_replay_adjustment_when_idempotency_key_reused() {
        var useCase = useCase();
        useCase.execute(command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("12.0000")), IDEMPOTENCY_KEY);

        var replayed = useCase.execute(command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("12.0000")), IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.movement().quantity()).isEqualByComparingTo("2.0000");
        assertThat(stockRepository.adjustmentRequests).hasSize(1);
        assertThat(stockRepository.stockMovements).hasSize(1);
        assertThat(stockRepository.stockBalances.get("IT-LAPTOP-001|" + WAREHOUSE_ID + "|PCS"))
                .isEqualByComparingTo("12.0000");
    }

    @Test
    void should_throw_inv011_when_adjustment_delta_is_zero() {
        var useCase = useCase();

        assertThatThrownBy(() -> useCase.execute(
                command(WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("10.0000")),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_011);
        assertThat(stockRepository.adjustmentRequests).isEmpty();
        assertThat(stockRepository.stockMovements).isEmpty();
    }

    @Test
    void should_throw_inv001_when_adjustment_item_is_missing() {
        var useCase = useCase();

        assertThatThrownBy(() -> useCase.execute(
                command(WAREHOUSE_ID, "UNKNOWN", new BigDecimal("1.0000")),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_001);
        assertThat(stockRepository.adjustmentRequests).isEmpty();
    }

    @Test
    void should_throw_inv002_when_adjustment_warehouse_is_missing() {
        var useCase = useCase();

        assertThatThrownBy(() -> useCase.execute(
                command(UNKNOWN_WAREHOUSE_ID, "IT-LAPTOP-001", new BigDecimal("1.0000")),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_002);
        assertThat(stockRepository.adjustmentRequests).isEmpty();
    }

    private AdjustStockUseCase useCase() {
        return new AdjustStockUseCase(
                stockRepository,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AdjustStockCommand command(UUID warehouseId, String itemCode, BigDecimal newQuantity) {
        return new AdjustStockCommand(
                ACTOR_ID,
                warehouseId,
                itemCode,
                newQuantity,
                "PCS",
                "Annual stock count correction");
    }

    private static final class FakeStockRepository implements StockRepository {
        private final Set<String> itemCodes = new HashSet<>();
        private final Map<String, String> itemNames = new LinkedHashMap<>();
        private final Set<UUID> warehouseIds = new HashSet<>();
        private final Map<String, BigDecimal> stockBalances = new LinkedHashMap<>();
        private final List<StockAdjustmentRequest> adjustmentRequests = new ArrayList<>();
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
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void insertIssueOutRequest(StockIssueOutRequest issueOutRequest) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public Optional<StockAdjustmentRequest> findAdjustmentRequestByIdempotencyKey(UUID idempotencyKey) {
            return adjustmentRequests.stream()
                    .filter(request -> request.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public void insertAdjustmentRequest(StockAdjustmentRequest adjustmentRequest) {
            adjustmentRequests.add(adjustmentRequest);
        }

        @Override
        public Optional<BigDecimal> findStockQuantity(String itemCode, UUID warehouseId, String unit) {
            return Optional.ofNullable(stockBalances.get(stockKey(itemCode, warehouseId, unit)));
        }

        @Override
        public Optional<BigDecimal> issueStock(
                String itemCode,
                UUID warehouseId,
                BigDecimal quantity,
                String unit,
                UUID actorId,
                Instant occurredAt) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public BigDecimal adjustStock(
                String itemCode,
                UUID warehouseId,
                BigDecimal newQuantity,
                String unit,
                UUID actorId,
                Instant occurredAt) {
            stockBalances.put(stockKey(itemCode, warehouseId, unit), newQuantity);
            return newQuantity;
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

        private String stockKey(String itemCode, UUID warehouseId, String unit) {
            return itemCode + "|" + warehouseId + "|" + unit;
        }
    }
}
