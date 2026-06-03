package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.inventory.application.port.in.GetItemStockQuery;
import com.eprocure.inventory.application.port.in.ListStockMovementsQuery;
import com.eprocure.inventory.application.port.in.ListWarehouseStockQuery;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.domain.model.StockEntry;
import com.eprocure.inventory.domain.model.StockMovementHistory;
import com.eprocure.inventory.domain.model.StockMovementType;
import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockQueryUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000002");
    private static final UUID UNKNOWN_WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000099");
    private static final UUID MOVEMENT_ID = UUID.fromString("82000000-0000-4000-8000-000000000001");
    private static final UUID SOURCE_REF_ID = UUID.fromString("83000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-03T04:00:00Z");

    private FakeStockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository = new FakeStockRepository();
        stockRepository.itemCodes.addAll(Set.of("IT-LAPTOP-001", "IT-MOUSE-001"));
        stockRepository.warehouseIds.addAll(Set.of(WAREHOUSE_ID, OTHER_WAREHOUSE_ID));
        stockRepository.stockEntries.addAll(List.of(
                new StockEntry(
                        "IT-LAPTOP-001",
                        "Laptop",
                        WAREHOUSE_ID,
                        "Main Warehouse",
                        new BigDecimal("3.0000"),
                        "PCS",
                        new BigDecimal("5.0000"),
                        true,
                        NOW),
                new StockEntry(
                        "IT-MOUSE-001",
                        "Mouse",
                        WAREHOUSE_ID,
                        "Main Warehouse",
                        new BigDecimal("12.0000"),
                        "PCS",
                        new BigDecimal("10.0000"),
                        false,
                        NOW)));
        stockRepository.movements.add(new StockMovementHistory(
                MOVEMENT_ID,
                "IT-LAPTOP-001",
                "Laptop",
                WAREHOUSE_ID,
                StockMovementType.RECEIPT_IN,
                new BigDecimal("3.0000"),
                "PCS",
                new BigDecimal("3.0000"),
                "GOODS_RECEIPT",
                SOURCE_REF_ID,
                ACTOR_ID,
                "Warehouse Keeper",
                NOW,
                "Receipt from GR-2026-000001"));
    }

    @Test
    void should_return_item_stock_when_item_exists() {
        var useCase = new GetItemStockUseCase(stockRepository);

        var result = useCase.execute(new GetItemStockQuery(ACTOR_ID, "IT-LAPTOP-001", null));

        assertThat(result).singleElement().satisfies(stock -> {
            assertThat(stock.itemCode()).isEqualTo("IT-LAPTOP-001");
            assertThat(stock.quantityOnHand()).isEqualByComparingTo("3.0000");
            assertThat(stock.belowReorder()).isTrue();
        });
    }

    @Test
    void should_throw_inv001_when_item_missing() {
        var useCase = new GetItemStockUseCase(stockRepository);

        assertThatThrownBy(() -> useCase.execute(new GetItemStockQuery(ACTOR_ID, "UNKNOWN", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_001);
    }

    @Test
    void should_throw_inv002_when_item_stock_warehouse_missing() {
        var useCase = new GetItemStockUseCase(stockRepository);

        assertThatThrownBy(() -> useCase.execute(new GetItemStockQuery(
                ACTOR_ID,
                "IT-LAPTOP-001",
                UNKNOWN_WAREHOUSE_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_002);
    }

    @Test
    void should_list_warehouse_stock_when_below_reorder_filter_matches() {
        var useCase = new ListWarehouseStockUseCase(stockRepository);

        var result = useCase.execute(new ListWarehouseStockQuery(ACTOR_ID, WAREHOUSE_ID, true, 1, 20));

        assertThat(result.items()).singleElement().satisfies(stock -> {
            assertThat(stock.itemCode()).isEqualTo("IT-LAPTOP-001");
            assertThat(stock.reorderPoint()).isEqualByComparingTo("5.0000");
        });
        assertThat(result.meta().totalElements()).isEqualTo(1);
        assertThat(result.meta().sort()).isEqualTo("itemName,asc");
    }

    @Test
    void should_throw_inv002_when_warehouse_stock_warehouse_missing() {
        var useCase = new ListWarehouseStockUseCase(stockRepository);

        assertThatThrownBy(() -> useCase.execute(new ListWarehouseStockQuery(
                ACTOR_ID,
                UNKNOWN_WAREHOUSE_ID,
                null,
                1,
                20)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_002);
    }

    @Test
    void should_list_stock_movements_when_filter_matches() {
        var useCase = new ListStockMovementsUseCase(stockRepository);

        var result = useCase.execute(new ListStockMovementsQuery(
                ACTOR_ID,
                "IT-LAPTOP-001",
                WAREHOUSE_ID,
                StockMovementType.RECEIPT_IN,
                LocalDate.parse("2026-06-03"),
                LocalDate.parse("2026-06-03"),
                1,
                20));

        assertThat(result.items()).singleElement().satisfies(movement -> {
            assertThat(movement.id()).isEqualTo(MOVEMENT_ID);
            assertThat(movement.quantity()).isEqualByComparingTo("3.0000");
            assertThat(movement.performedBy().id()).isEqualTo(ACTOR_ID);
        });
        assertThat(result.meta().totalElements()).isEqualTo(1);
        assertThat(result.meta().sort()).isEqualTo("performedAt,desc");
    }

    @Test
    void should_throw_inv001_when_stock_movement_item_filter_missing() {
        var useCase = new ListStockMovementsUseCase(stockRepository);

        assertThatThrownBy(() -> useCase.execute(new ListStockMovementsQuery(
                ACTOR_ID,
                "UNKNOWN",
                null,
                null,
                null,
                null,
                1,
                20)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_001);
    }

    private static final class FakeStockRepository implements StockRepository {
        private final Set<String> itemCodes = new HashSet<>();
        private final Set<UUID> warehouseIds = new HashSet<>();
        private final List<StockEntry> stockEntries = new java.util.ArrayList<>();
        private final List<StockMovementHistory> movements = new java.util.ArrayList<>();

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
            return filterStockEntries(filter).stream()
                    .sorted(Comparator.comparing(StockEntry::itemName)
                            .thenComparing(StockEntry::itemCode)
                            .thenComparing(StockEntry::warehouseName))
                    .skip(filter.offset())
                    .limit(filter.size())
                    .toList();
        }

        @Override
        public long countStockEntries(StockEntryFilter filter) {
            return filterStockEntries(filter).size();
        }

        @Override
        public List<StockMovementHistory> findMovements(StockMovementFilter filter) {
            return filterMovements(filter).stream()
                    .sorted(Comparator.comparing(StockMovementHistory::performedAt).reversed()
                            .thenComparing(StockMovementHistory::id))
                    .skip(filter.offset())
                    .limit(filter.size())
                    .toList();
        }

        @Override
        public long countMovements(StockMovementFilter filter) {
            return filterMovements(filter).size();
        }

        private List<StockEntry> filterStockEntries(StockEntryFilter filter) {
            return stockEntries.stream()
                    .filter(stock -> filter.itemCode() == null || stock.itemCode().equals(filter.itemCode()))
                    .filter(stock -> filter.warehouseId() == null || stock.warehouseId().equals(filter.warehouseId()))
                    .filter(stock -> filter.belowReorder() == null || stock.belowReorder() == filter.belowReorder())
                    .toList();
        }

        private List<StockMovementHistory> filterMovements(StockMovementFilter filter) {
            return movements.stream()
                    .filter(movement -> filter.itemCode() == null || movement.itemCode().equals(filter.itemCode()))
                    .filter(movement -> filter.warehouseId() == null || movement.warehouseId().equals(filter.warehouseId()))
                    .filter(movement -> filter.movementType() == null || movement.movementType() == filter.movementType())
                    .filter(movement -> filter.fromPerformedAt() == null || !movement.performedAt().isBefore(filter.fromPerformedAt()))
                    .filter(movement -> filter.toPerformedAtExclusive() == null
                            || movement.performedAt().isBefore(filter.toPerformedAtExclusive()))
                    .toList();
        }
    }
}
