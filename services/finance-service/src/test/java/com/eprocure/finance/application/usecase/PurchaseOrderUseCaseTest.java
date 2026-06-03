package com.eprocure.finance.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.finance.application.port.in.CreatePurchaseOrderFromRfqAwardCommand;
import com.eprocure.finance.application.port.in.GetPurchaseOrderQuery;
import com.eprocure.finance.application.port.in.ListPurchaseOrdersQuery;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.PurchaseOrderFilter;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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

class PurchaseOrderUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-03T02:30:00Z");
    private static final UUID RFQ_ID = UUID.fromString("91000000-0000-4000-8000-000000000001");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("92000000-0000-4000-8000-000000000001");
    private static final UUID QUOTE_ID = UUID.fromString("93000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID RFQ_LINE_ITEM_ID = UUID.fromString("94000000-0000-4000-8000-000000000001");
    private static final UUID PR_LINE_ITEM_ID = UUID.fromString("95000000-0000-4000-8000-000000000001");

    private FakePurchaseOrderRepository purchaseOrderRepository;

    @BeforeEach
    void setUp() {
        purchaseOrderRepository = new FakePurchaseOrderRepository();
    }

    @Test
    void should_create_draft_po_when_rfq_awarded_event_arrives() {
        var useCase = new CreatePurchaseOrderFromRfqAwardUseCase(
                purchaseOrderRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(command("evt-rfq-awarded-001"));

        assertThat(result).isPresent();
        assertThat(result.get().poNumber()).isEqualTo("PO-2026-000001");
        assertThat(result.get().status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(result.get().lineItems()).hasSize(1);
        assertThat(result.get().totalAmount().amount()).isEqualByComparingTo("25000000.0000");
        assertThat(purchaseOrderRepository.purchaseOrders).hasSize(1);
        assertThat(purchaseOrderRepository.processedEvents).contains("evt-rfq-awarded-001");
    }

    @Test
    void should_skip_create_when_rfq_awarded_event_is_replayed() {
        var useCase = new CreatePurchaseOrderFromRfqAwardUseCase(
                purchaseOrderRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        useCase.execute(command("evt-rfq-awarded-001"));

        var replayed = useCase.execute(command("evt-rfq-awarded-001"));

        assertThat(replayed).isPresent();
        assertThat(purchaseOrderRepository.purchaseOrders).hasSize(1);
        assertThat(purchaseOrderRepository.generatedPoNumbers).isEqualTo(1);
    }

    @Test
    void should_list_only_own_purchase_orders_when_user_has_own_permission() {
        purchaseOrderRepository.purchaseOrders.add(purchaseOrder(ACTOR_ID, VENDOR_ID));
        purchaseOrderRepository.purchaseOrders.add(purchaseOrder(OTHER_ACTOR_ID, UUID.fromString("92000000-0000-4000-8000-000000000002")));
        var useCase = new ListPurchaseOrdersUseCase(purchaseOrderRepository);

        var result = useCase.execute(new ListPurchaseOrdersQuery(
                ACTOR_ID,
                Set.of("PO_VIEW_OWN"),
                null,
                null,
                null,
                null,
                1,
                20,
                null));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).purchasingOfficer().id()).isEqualTo(ACTOR_ID);
    }

    @Test
    void should_return_po_when_user_has_view_all_permission() {
        PurchaseOrder purchaseOrder = purchaseOrder(OTHER_ACTOR_ID, VENDOR_ID);
        purchaseOrderRepository.purchaseOrders.add(purchaseOrder);
        var useCase = new GetPurchaseOrderUseCase(purchaseOrderRepository);

        var result = useCase.execute(new GetPurchaseOrderQuery(
                ACTOR_ID,
                Set.of("PO_VIEW_ALL"),
                purchaseOrder.id()));

        assertThat(result.id()).isEqualTo(purchaseOrder.id());
    }

    @Test
    void should_throw_iam004_when_user_reads_po_owned_by_other_actor() {
        PurchaseOrder purchaseOrder = purchaseOrder(OTHER_ACTOR_ID, VENDOR_ID);
        purchaseOrderRepository.purchaseOrders.add(purchaseOrder);
        var useCase = new GetPurchaseOrderUseCase(purchaseOrderRepository);

        assertThatThrownBy(() -> useCase.execute(new GetPurchaseOrderQuery(
                ACTOR_ID,
                Set.of("PO_VIEW_OWN"),
                purchaseOrder.id())))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IAM_004);
    }

    private static CreatePurchaseOrderFromRfqAwardCommand command(String eventId) {
        return new CreatePurchaseOrderFromRfqAwardCommand(
                eventId,
                "procurement.rfq.awarded",
                0,
                10L,
                RFQ_ID,
                "RFQ-2026-000001",
                PR_ID,
                "PR-2026-000001",
                VENDOR_ID,
                "Acme Supplier",
                "sales@acme.example",
                "ACME-TAX",
                QUOTE_ID,
                money("25000000.0000"),
                "NET30",
                ACTOR_ID,
                NOW,
                List.of(new CreatePurchaseOrderFromRfqAwardCommand.LineItem(
                        RFQ_LINE_ITEM_ID,
                        PR_LINE_ITEM_ID,
                        "Laptop",
                        "IT",
                        new BigDecimal("10.0000"),
                        "PCS",
                        money("2500000.0000"),
                        money("25000000.0000"),
                        7,
                        "12 months")));
    }

    private static PurchaseOrder purchaseOrder(UUID purchasingOfficerId, UUID vendorId) {
        UUID id = UUID.randomUUID();
        return new PurchaseOrder(
                id,
                "PO-2026-" + id.toString().substring(0, 6).toUpperCase(),
                PR_ID,
                "PR-2026-000001",
                RFQ_ID,
                "RFQ-2026-000001",
                QUOTE_ID,
                vendorId,
                "Acme Supplier",
                "sales@acme.example",
                "ACME-TAX",
                purchasingOfficerId,
                null,
                PurchaseOrderStatus.DRAFT,
                List.of(new PurchaseOrderLineItem(
                        UUID.randomUUID(),
                        1,
                        RFQ_LINE_ITEM_ID,
                        PR_LINE_ITEM_ID,
                        "Laptop",
                        "IT",
                        new BigDecimal("10.0000"),
                        "PCS",
                        money("2500000.0000"),
                        money("25000000.0000"),
                        7,
                        "12 months")),
                money("25000000.0000"),
                null,
                null,
                "NET30",
                null,
                null,
                NOW,
                "evt-" + id);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "VND");
    }

    private static final class FakePurchaseOrderRepository implements PurchaseOrderRepository {
        private final List<PurchaseOrder> purchaseOrders = new ArrayList<>();
        private final Set<String> processedEvents = new HashSet<>();
        private int generatedPoNumbers;

        @Override
        public Optional<PurchaseOrder> findById(UUID poId) {
            return purchaseOrders.stream()
                    .filter(purchaseOrder -> purchaseOrder.id().equals(poId))
                    .findFirst();
        }

        @Override
        public Optional<PurchaseOrder> findBySourceEventId(String sourceEventId) {
            return purchaseOrders.stream()
                    .filter(purchaseOrder -> sourceEventId.equals(purchaseOrder.sourceEventId()))
                    .findFirst();
        }

        @Override
        public Optional<PurchaseOrder> findByRfqId(UUID rfqId) {
            return purchaseOrders.stream()
                    .filter(purchaseOrder -> rfqId.equals(purchaseOrder.rfqId()))
                    .findFirst();
        }

        @Override
        public List<PurchaseOrder> findByFilter(PurchaseOrderFilter filter) {
            return purchaseOrders.stream()
                    .filter(purchaseOrder -> filter.purchasingOfficerId() == null
                            || filter.purchasingOfficerId().equals(purchaseOrder.purchasingOfficerId()))
                    .filter(purchaseOrder -> filter.status() == null || filter.status() == purchaseOrder.status())
                    .filter(purchaseOrder -> filter.vendorId() == null || filter.vendorId().equals(purchaseOrder.vendorId()))
                    .sorted(Comparator.comparing(PurchaseOrder::createdAt).reversed())
                    .skip(filter.offset())
                    .limit(filter.size())
                    .toList();
        }

        @Override
        public long countByFilter(PurchaseOrderFilter filter) {
            return findByFilter(new PurchaseOrderFilter(
                    filter.purchasingOfficerId(),
                    filter.status(),
                    filter.vendorId(),
                    filter.fromCreatedAt(),
                    filter.toCreatedAtExclusive(),
                    1,
                    Integer.MAX_VALUE,
                    0,
                    filter.sortField(),
                    filter.sortDirection())).size();
        }

        @Override
        public String nextPoNumber(int fiscalYear) {
            generatedPoNumbers++;
            return "PO-" + fiscalYear + "-" + String.format("%06d", generatedPoNumbers);
        }

        @Override
        public void insert(PurchaseOrder purchaseOrder) {
            purchaseOrders.add(purchaseOrder);
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return processedEvents.contains(eventId);
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            processedEvents.add(eventId);
        }
    }
}
