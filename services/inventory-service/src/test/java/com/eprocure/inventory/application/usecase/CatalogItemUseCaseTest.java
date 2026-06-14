package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.inventory.application.port.in.CreateItemCommand;
import com.eprocure.inventory.application.port.in.GetItemDetailQuery;
import com.eprocure.inventory.application.port.in.SearchItemsQuery;
import com.eprocure.inventory.application.port.in.UpdateItemCommand;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.domain.model.CatalogItemMutationRequest;
import com.eprocure.inventory.domain.model.Item;
import com.eprocure.inventory.domain.repository.ItemFilter;
import com.eprocure.inventory.domain.repository.ItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogItemUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-12T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID WAREHOUSE_ID = UUID.fromString("81000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("80000000-0000-0000-0000-000000000101");
    private static final String CREATE_IDEMPOTENCY_KEY = "55555555-5555-4555-8555-555555555555";
    private static final String UPDATE_IDEMPOTENCY_KEY = "66666666-6666-4666-8666-666666666666";

    private FakeItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository = new FakeItemRepository();
        itemRepository.items.add(sampleItem("IT-LAPTOP-001", true));
        itemRepository.stockSummaries.add(new ItemRepository.StockSummary(
                WAREHOUSE_ID,
                "Main Warehouse",
                new BigDecimal("4.0000"),
                "pcs"));
    }

    @Test
    void should_create_item_when_valid_command() {
        var useCase = createUseCase();

        var result = useCase.execute(new CreateItemCommand(
                ACTOR_ID,
                "IT-MOUSE-001",
                "Wireless Mouse",
                "Office mouse",
                "IT",
                "pcs",
                new BigDecimal("250000.0000"),
                "VND",
                VENDOR_ID,
                new BigDecimal("10.0000")), CREATE_IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.item().itemCode()).isEqualTo("IT-MOUSE-001");
        assertThat(result.item().unitPrice()).isEqualByComparingTo("250000.0000");
        assertThat(itemRepository.items).hasSize(2);
        assertThat(itemRepository.mutationRequests).hasSize(1);
    }

    @Test
    void should_throw_inv007_when_item_code_exists() {
        var useCase = createUseCase();

        assertThatThrownBy(() -> useCase.execute(new CreateItemCommand(
                ACTOR_ID,
                "IT-LAPTOP-001",
                "Laptop",
                null,
                "IT",
                "pcs",
                new BigDecimal("1000.0000"),
                "VND",
                null,
                null), CREATE_IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_007);
    }

    @Test
    void should_replay_create_item_when_idempotency_key_reused() {
        var useCase = createUseCase();
        useCase.execute(new CreateItemCommand(
                ACTOR_ID,
                "IT-MOUSE-001",
                "Wireless Mouse",
                null,
                "IT",
                "pcs",
                new BigDecimal("250000.0000"),
                "VND",
                null,
                null), CREATE_IDEMPOTENCY_KEY);

        var replayed = useCase.execute(new CreateItemCommand(
                ACTOR_ID,
                "IT-MOUSE-001",
                "Wireless Mouse",
                null,
                "IT",
                "pcs",
                new BigDecimal("250000.0000"),
                "VND",
                null,
                null), CREATE_IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(itemRepository.items).hasSize(2);
        assertThat(itemRepository.mutationRequests).hasSize(1);
    }

    @Test
    void should_get_item_detail_when_existing() {
        var useCase = new GetItemDetailUseCase(itemRepository);

        var result = useCase.execute(new GetItemDetailQuery(ACTOR_ID, "IT-LAPTOP-001"));

        assertThat(result.itemCode()).isEqualTo("IT-LAPTOP-001");
        assertThat(result.stockSummary()).singleElement().satisfies(stock -> {
            assertThat(stock.warehouseId()).isEqualTo(WAREHOUSE_ID);
            assertThat(stock.quantityOnHand()).isEqualByComparingTo("4.0000");
        });
    }

    @Test
    void should_throw_inv001_when_item_missing() {
        var useCase = new GetItemDetailUseCase(itemRepository);

        assertThatThrownBy(() -> useCase.execute(new GetItemDetailQuery(ACTOR_ID, "UNKNOWN")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INV_001);
    }

    @Test
    void should_search_items_when_filter_matches() {
        itemRepository.items.add(sampleItem("OFFICE-PAPER-001", false));
        var useCase = new SearchItemsUseCase(itemRepository);

        var result = useCase.execute(new SearchItemsQuery(
                ACTOR_ID,
                "laptop",
                "IT",
                true,
                true,
                1,
                20));

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.itemCode()).isEqualTo("IT-LAPTOP-001");
            assertThat(item.stockSummary()).hasSize(1);
        });
        assertThat(result.meta().totalElements()).isEqualTo(1);
    }

    @Test
    void should_update_item_when_valid_command() {
        var useCase = updateUseCase();

        var result = useCase.execute(new UpdateItemCommand(
                ACTOR_ID,
                "IT-LAPTOP-001",
                "Business Laptop",
                "Updated description",
                new BigDecimal("32000000.0000"),
                "VND",
                VENDOR_ID,
                new BigDecimal("3.0000"),
                false), UPDATE_IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.item().name()).isEqualTo("Business Laptop");
        assertThat(result.item().unitPrice()).isEqualByComparingTo("32000000.0000");
        assertThat(result.item().reorderPoint()).isEqualByComparingTo("3.0000");
        assertThat(result.item().active()).isFalse();
    }

    @Test
    void should_replay_update_item_when_idempotency_key_reused() {
        var useCase = updateUseCase();
        useCase.execute(new UpdateItemCommand(
                ACTOR_ID,
                "IT-LAPTOP-001",
                "Business Laptop",
                null,
                new BigDecimal("32000000.0000"),
                "VND",
                null,
                null,
                true), UPDATE_IDEMPOTENCY_KEY);

        var replayed = useCase.execute(new UpdateItemCommand(
                ACTOR_ID,
                "IT-LAPTOP-001",
                "Business Laptop",
                null,
                new BigDecimal("32000000.0000"),
                "VND",
                null,
                null,
                true), UPDATE_IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(itemRepository.mutationRequests).hasSize(1);
    }

    private CreateItemUseCase createUseCase() {
        return new CreateItemUseCase(
                itemRepository,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private UpdateItemUseCase updateUseCase() {
        return new UpdateItemUseCase(
                itemRepository,
                new IdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Item sampleItem(String itemCode, boolean active) {
        return new Item(
                UUID.randomUUID(),
                itemCode,
                itemCode.equals("IT-LAPTOP-001") ? "Business Laptop" : "Office Paper",
                null,
                itemCode.startsWith("IT") ? "IT" : "OFFICE",
                "pcs",
                new BigDecimal("30000000.0000"),
                "VND",
                null,
                new BigDecimal("5.0000"),
                active,
                NOW,
                ACTOR_ID,
                ACTOR_ID);
    }

    private static class FakeItemRepository implements ItemRepository {
        private final List<Item> items = new ArrayList<>();
        private final List<CatalogItemMutationRequest> mutationRequests = new ArrayList<>();
        private final List<StockSummary> stockSummaries = new ArrayList<>();

        @Override
        public Optional<Item> findByCode(String itemCode) {
            return items.stream()
                    .filter(item -> item.itemCode().equals(normalize(itemCode)))
                    .findFirst();
        }

        @Override
        public Optional<CatalogItemMutationRequest> findMutationRequestByIdempotencyKey(UUID idempotencyKey) {
            return mutationRequests.stream()
                    .filter(request -> request.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public List<Item> findByFilter(ItemFilter filter) {
            return items.stream()
                    .filter(item -> filter.query() == null
                            || item.name().toLowerCase(Locale.ROOT).contains(filter.query().toLowerCase(Locale.ROOT))
                            || item.itemCode().toLowerCase(Locale.ROOT).contains(filter.query().toLowerCase(Locale.ROOT)))
                    .filter(item -> filter.categoryCode() == null || item.categoryCode().equals(filter.categoryCode()))
                    .filter(item -> filter.active() == null || item.active() == filter.active())
                    .filter(item -> filter.belowReorder() == null || isBelowReorder(item) == filter.belowReorder())
                    .skip(filter.offset())
                    .limit(filter.size())
                    .toList();
        }

        @Override
        public long countByFilter(ItemFilter filter) {
            return findByFilter(new ItemFilter(
                    filter.query(),
                    filter.categoryCode(),
                    filter.active(),
                    filter.belowReorder(),
                    1,
                    1000,
                    0)).size();
        }

        @Override
        public List<StockSummary> findStockSummary(String itemCode) {
            return findByCode(itemCode).isPresent() ? List.copyOf(stockSummaries) : List.of();
        }

        @Override
        public boolean existsActiveCode(String itemCode) {
            return findByCode(itemCode).isPresent();
        }

        @Override
        public void insert(Item item) {
            items.add(item);
        }

        @Override
        public boolean update(Item item) {
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).itemCode().equals(item.itemCode())) {
                    items.set(index, item);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void insertMutationRequest(CatalogItemMutationRequest request) {
            mutationRequests.add(request);
        }

        private boolean isBelowReorder(Item item) {
            if (item.reorderPoint() == null) {
                return false;
            }
            BigDecimal total = stockSummaries.stream()
                    .map(StockSummary::quantityOnHand)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.compareTo(item.reorderPoint()) < 0;
        }

        private String normalize(String value) {
            return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        }
    }
}
