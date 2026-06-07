package com.eprocure.finance.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.finance.application.port.in.CreateInvoiceCommand;
import com.eprocure.finance.application.port.in.ApproveInvoiceCommand;
import com.eprocure.finance.application.port.in.ConfirmPaymentCommand;
import com.eprocure.finance.application.port.in.DisputeInvoiceCommand;
import com.eprocure.finance.application.port.in.GetInvoiceQuery;
import com.eprocure.finance.application.port.in.ListInvoicesQuery;
import com.eprocure.finance.application.port.in.MatchInvoiceCommand;
import com.eprocure.finance.application.port.out.InvoiceMatchedEventPublisher;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.domain.event.InvoiceMatchedEvent;
import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.domain.model.Payment;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.GoodsReceiptSnapshotRepository;
import com.eprocure.finance.domain.repository.InvoiceFilter;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import com.eprocure.finance.domain.repository.PaymentRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderFilter;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.repository.BudgetFilter;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-03T06:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID PO_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("71000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_VENDOR_ID = UUID.fromString("71000000-0000-4000-8000-000000000099");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final UUID RFQ_ID = UUID.fromString("91000000-0000-4000-8000-000000000001");
    private static final UUID QUOTE_ID = UUID.fromString("92000000-0000-4000-8000-000000000001");
    private static final UUID PO_LINE_ITEM_ID = UUID.fromString("73000000-0000-4000-8000-000000000001");
    private static final UUID PR_LINE_ITEM_ID = UUID.fromString("95000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "55555555-5555-4555-8555-555555555555";
    private static final String MATCH_IDEMPOTENCY_KEY = "66666666-6666-4666-8666-666666666666";
    private static final String APPROVE_IDEMPOTENCY_KEY = "77777777-7777-4777-8777-777777777777";
    private static final String DISPUTE_IDEMPOTENCY_KEY = "88888888-8888-4888-8888-888888888888";
    private static final String PAYMENT_IDEMPOTENCY_KEY = "99999999-9999-4999-8999-999999999999";

    private FakeInvoiceRepository invoiceRepository;
    private FakePurchaseOrderRepository purchaseOrderRepository;
    private FakeGoodsReceiptSnapshotRepository goodsReceiptSnapshotRepository;
    private FakeInvoiceMatchedEventPublisher invoiceMatchedEventPublisher;
    private FakePaymentRepository paymentRepository;
    private FakeBudgetRepository budgetRepository;
    private FakeBudgetDashboardCachePort budgetDashboardCachePort;

    @BeforeEach
    void setUp() {
        invoiceRepository = new FakeInvoiceRepository();
        purchaseOrderRepository = new FakePurchaseOrderRepository();
        goodsReceiptSnapshotRepository = new FakeGoodsReceiptSnapshotRepository();
        invoiceMatchedEventPublisher = new FakeInvoiceMatchedEventPublisher();
        paymentRepository = new FakePaymentRepository();
        budgetRepository = new FakeBudgetRepository();
        budgetDashboardCachePort = new FakeBudgetDashboardCachePort();
        purchaseOrderRepository.purchaseOrders.add(purchaseOrder());
    }

    @Test
    void should_create_invoice_when_purchase_order_exists() {
        var useCase = createUseCase();

        var result = useCase.execute(command(VENDOR_ID), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.view().invoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(result.view().status()).isEqualTo(InvoiceStatus.PENDING_MATCH);
        assertThat(result.view().vendor().id()).isEqualTo(VENDOR_ID);
        assertThat(result.view().po().id()).isEqualTo(PO_ID);
        assertThat(result.view().subtotal().amount()).isEqualByComparingTo("1000.0000");
        assertThat(result.view().taxAmount().amount()).isEqualByComparingTo("100.0000");
        assertThat(result.view().totalAmount().amount()).isEqualByComparingTo("1100.0000");
        assertThat(invoiceRepository.invoices).hasSize(1);
    }

    @Test
    void should_replay_invoice_when_idempotency_key_reused() {
        var useCase = createUseCase();
        useCase.execute(command(VENDOR_ID), IDEMPOTENCY_KEY);

        var replayed = useCase.execute(command(VENDOR_ID), IDEMPOTENCY_KEY);

        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.view().invoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(invoiceRepository.invoices).hasSize(1);
    }

    @Test
    void should_throw_fin006_when_purchase_order_missing() {
        purchaseOrderRepository.purchaseOrders.clear();
        var useCase = createUseCase();

        assertThatThrownBy(() -> useCase.execute(command(VENDOR_ID), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FIN_006);
    }

    @Test
    void should_throw_fin010_when_vendor_does_not_match_purchase_order() {
        var useCase = createUseCase();

        assertThatThrownBy(() -> useCase.execute(command(OTHER_VENDOR_ID), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FIN_010);
    }

    @Test
    void should_get_invoice_when_existing() {
        createUseCase().execute(command(VENDOR_ID), IDEMPOTENCY_KEY);
        Invoice invoice = invoiceRepository.invoices.get(0);
        var useCase = new GetInvoiceUseCase(invoiceRepository);

        var result = useCase.execute(new GetInvoiceQuery(ACTOR_ID, invoice.id()));

        assertThat(result.id()).isEqualTo(invoice.id());
        assertThat(result.lineItems()).hasSize(1);
    }

    @Test
    void should_list_invoices_when_filter_matches() {
        createUseCase().execute(command(VENDOR_ID), IDEMPOTENCY_KEY);
        var useCase = new ListInvoicesUseCase(invoiceRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(new ListInvoicesQuery(
                ACTOR_ID,
                InvoiceStatus.PENDING_MATCH,
                VENDOR_ID,
                PO_ID,
                false,
                1,
                20));

        assertThat(result.items()).hasSize(1);
        assertThat(result.meta().totalElements()).isEqualTo(1);
        assertThat(result.meta().sort()).isEqualTo("createdAt,desc");
    }

    @Test
    void should_match_invoice_when_po_and_gr_quantities_match() {
        createUseCase().execute(command(VENDOR_ID), IDEMPOTENCY_KEY);
        Invoice invoice = invoiceRepository.invoices.get(0);
        goodsReceiptSnapshotRepository.receivedQuantities.put(PO_LINE_ITEM_ID, new BigDecimal("2.0000"));
        var useCase = matchUseCase();

        var result = useCase.execute(new MatchInvoiceCommand(ACTOR_ID, invoice.id()), MATCH_IDEMPOTENCY_KEY);

        assertThat(result.matchStatus()).isEqualTo(MatchStatus.MATCHED);
        assertThat(result.requiresManualReview()).isFalse();
        assertThat(invoiceRepository.findById(invoice.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.MATCHED);
        assertThat(invoiceMatchedEventPublisher.events).hasSize(1);
    }

    @Test
    void should_mark_invoice_mismatched_when_gr_quantity_missing() {
        createUseCase().execute(command(VENDOR_ID), IDEMPOTENCY_KEY);
        Invoice invoice = invoiceRepository.invoices.get(0);
        var useCase = matchUseCase();

        var result = useCase.execute(new MatchInvoiceCommand(ACTOR_ID, invoice.id()), MATCH_IDEMPOTENCY_KEY);

        assertThat(result.matchStatus()).isEqualTo(MatchStatus.MISMATCHED);
        assertThat(result.requiresManualReview()).isTrue();
        assertThat(invoiceRepository.findById(invoice.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.MISMATCHED);
        assertThat(invoiceMatchedEventPublisher.events).isEmpty();
    }

    @Test
    void should_approve_invoice_when_invoice_is_matched() {
        Invoice invoice = matchedInvoice();
        var useCase = new ApproveInvoiceUseCase(
                invoiceRepository,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(
                new ApproveInvoiceCommand(ACTOR_ID, invoice.id(), "ready to pay"),
                APPROVE_IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.status()).isEqualTo(InvoiceStatus.APPROVED);
        assertThat(invoiceRepository.findById(invoice.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.APPROVED);
    }

    @Test
    void should_dispute_invoice_when_invoice_is_mismatched() {
        createUseCase().execute(command(VENDOR_ID), IDEMPOTENCY_KEY);
        Invoice invoice = invoiceRepository.invoices.get(0);
        matchUseCase().execute(new MatchInvoiceCommand(ACTOR_ID, invoice.id()), MATCH_IDEMPOTENCY_KEY);
        var useCase = new DisputeInvoiceUseCase(
                invoiceRepository,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(
                new DisputeInvoiceCommand(ACTOR_ID, invoice.id(), "Received quantity is missing for this supplier invoice"),
                DISPUTE_IDEMPOTENCY_KEY);

        assertThat(result.status()).isEqualTo(InvoiceStatus.DISPUTED);
        assertThat(invoiceRepository.findById(invoice.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.DISPUTED);
    }

    @Test
    void should_confirm_payment_when_invoice_is_approved() {
        Invoice invoice = matchedInvoice();
        new ApproveInvoiceUseCase(invoiceRepository, new FakeIdempotencyService(), Clock.fixed(NOW, ZoneOffset.UTC))
                .execute(new ApproveInvoiceCommand(ACTOR_ID, invoice.id(), null), APPROVE_IDEMPOTENCY_KEY);

        UUID budgetId = UUID.randomUUID();
        budgetRepository.hold = new BudgetCommitmentHold(budgetId, new Money(new BigDecimal("1100.0000"), "VND"));

        var useCase = new ConfirmPaymentUseCase(
                invoiceRepository,
                paymentRepository,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                purchaseOrderRepository,
                budgetRepository,
                budgetDashboardCachePort);

        var result = useCase.execute(
                new ConfirmPaymentCommand(
                        ACTOR_ID,
                        invoice.id(),
                        LocalDate.parse("2026-06-10"),
                        "BANK-REF-001",
                        new BigDecimal("1100.0000"),
                        "paid by transfer"),
                PAYMENT_IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.payment().paidAmount().amount()).isEqualByComparingTo("1100.0000");
        assertThat(invoiceRepository.findById(invoice.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paymentRepository.payments).hasSize(1);

        // Assert budget updates
        assertThat(budgetRepository.locked).isTrue();
        assertThat(budgetRepository.transactions).hasSize(2);

        var releaseTx = budgetRepository.transactions.get(0);
        assertThat(releaseTx.budgetId()).isEqualTo(budgetId);
        assertThat(releaseTx.transactionType()).isEqualTo(BudgetTransactionType.RELEASE);
        assertThat(releaseTx.money().amount()).isEqualByComparingTo("1100.0000");
        assertThat(releaseTx.referenceType()).isEqualTo("PURCHASE_REQUEST");
        assertThat(releaseTx.referenceId()).isEqualTo(PR_ID);

        var spendTx = budgetRepository.transactions.get(1);
        assertThat(spendTx.budgetId()).isEqualTo(budgetId);
        assertThat(spendTx.transactionType()).isEqualTo(BudgetTransactionType.SPEND);
        assertThat(spendTx.money().amount()).isEqualByComparingTo("1100.0000");
        assertThat(spendTx.referenceType()).isEqualTo("INVOICE");
        assertThat(spendTx.referenceId()).isEqualTo(invoice.id());

        assertThat(budgetDashboardCachePort.evictedBudgetIds).containsExactly(budgetId);
    }

    @Test
    void should_confirm_payment_without_budget_update_when_hold_missing() {
        Invoice invoice = matchedInvoice();
        new ApproveInvoiceUseCase(invoiceRepository, new FakeIdempotencyService(), Clock.fixed(NOW, ZoneOffset.UTC))
                .execute(new ApproveInvoiceCommand(ACTOR_ID, invoice.id(), null), APPROVE_IDEMPOTENCY_KEY);

        budgetRepository.hold = null;

        var useCase = new ConfirmPaymentUseCase(
                invoiceRepository,
                paymentRepository,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                purchaseOrderRepository,
                budgetRepository,
                budgetDashboardCachePort);

        var result = useCase.execute(
                new ConfirmPaymentCommand(
                        ACTOR_ID,
                        invoice.id(),
                        LocalDate.parse("2026-06-10"),
                        "BANK-REF-001",
                        new BigDecimal("1100.0000"),
                        "paid by transfer"),
                PAYMENT_IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(invoiceRepository.findById(invoice.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paymentRepository.payments).hasSize(1);
        assertThat(budgetRepository.locked).isFalse();
        assertThat(budgetRepository.transactions).isEmpty();
        assertThat(budgetDashboardCachePort.evictedBudgetIds).isEmpty();
    }

    @Test
    void should_reject_second_payment_when_invoice_already_paid_with_different_key() {
        Invoice invoice = matchedInvoice();
        new ApproveInvoiceUseCase(invoiceRepository, new FakeIdempotencyService(), Clock.fixed(NOW, ZoneOffset.UTC))
                .execute(new ApproveInvoiceCommand(ACTOR_ID, invoice.id(), null), APPROVE_IDEMPOTENCY_KEY);

        UUID budgetId = UUID.randomUUID();
        budgetRepository.hold = new BudgetCommitmentHold(budgetId, new Money(new BigDecimal("1100.0000"), "VND"));
        var useCase = new ConfirmPaymentUseCase(
                invoiceRepository,
                paymentRepository,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                purchaseOrderRepository,
                budgetRepository,
                budgetDashboardCachePort);

        useCase.execute(
                new ConfirmPaymentCommand(
                        ACTOR_ID,
                        invoice.id(),
                        LocalDate.parse("2026-06-10"),
                        "BANK-REF-001",
                        new BigDecimal("1100.0000"),
                        "paid by transfer"),
                PAYMENT_IDEMPOTENCY_KEY);

        assertThatThrownBy(() -> useCase.execute(
                new ConfirmPaymentCommand(
                        ACTOR_ID,
                        invoice.id(),
                        LocalDate.parse("2026-06-10"),
                        "BANK-REF-002",
                        new BigDecimal("1100.0000"),
                        "duplicate submit"),
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FIN_015));

        assertThat(paymentRepository.payments).hasSize(1);
        assertThat(budgetRepository.transactions).hasSize(2);
        assertThat(budgetDashboardCachePort.evictedBudgetIds).containsExactly(budgetId);
    }

    private CreateInvoiceUseCase createUseCase() {
        return new CreateInvoiceUseCase(
                invoiceRepository,
                purchaseOrderRepository,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private MatchInvoiceUseCase matchUseCase() {
        return new MatchInvoiceUseCase(
                invoiceRepository,
                purchaseOrderRepository,
                goodsReceiptSnapshotRepository,
                invoiceMatchedEventPublisher,
                new FakeIdempotencyService(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Invoice matchedInvoice() {
        createUseCase().execute(command(VENDOR_ID), IDEMPOTENCY_KEY);
        Invoice invoice = invoiceRepository.invoices.get(0);
        goodsReceiptSnapshotRepository.receivedQuantities.put(PO_LINE_ITEM_ID, new BigDecimal("2.0000"));
        matchUseCase().execute(new MatchInvoiceCommand(ACTOR_ID, invoice.id()), MATCH_IDEMPOTENCY_KEY);
        return invoiceRepository.findById(invoice.id()).orElseThrow();
    }

    private static CreateInvoiceCommand command(UUID vendorId) {
        return new CreateInvoiceCommand(
                ACTOR_ID,
                "INV-2026-0001",
                vendorId,
                PO_ID,
                LocalDate.parse("2026-06-03"),
                LocalDate.parse("2026-06-30"),
                List.of(new CreateInvoiceCommand.LineItem(
                        PO_LINE_ITEM_ID,
                        "Laptop",
                        new BigDecimal("2.0000"),
                        new BigDecimal("500.0000"),
                        new BigDecimal("0.1000"))));
    }

    private static PurchaseOrder purchaseOrder() {
        return new PurchaseOrder(
                PO_ID,
                "PO-2026-000001",
                PR_ID,
                "PR-2026-000001",
                RFQ_ID,
                "RFQ-2026-000001",
                QUOTE_ID,
                VENDOR_ID,
                "Acme Supplier",
                "sales@acme.example",
                "ACME-TAX",
                ACTOR_ID,
                "Purchasing Officer",
                PurchaseOrderStatus.SENT_TO_VENDOR,
                List.of(new PurchaseOrderLineItem(
                        PO_LINE_ITEM_ID,
                        1,
                        UUID.fromString("74000000-0000-4000-8000-000000000001"),
                        PR_LINE_ITEM_ID,
                        "Laptop",
                        "IT",
                        new BigDecimal("2.0000"),
                        "PCS",
                        new Money(new BigDecimal("500.0000"), "VND"),
                        new Money(new BigDecimal("1000.0000"), "VND"),
                        7,
                        null)),
                new Money(new BigDecimal("1000.0000"), "VND"),
                "Floor 10",
                LocalDate.parse("2026-06-30"),
                "NET30",
                null,
                NOW,
                NOW,
                null,
                null,
                null,
                NOW,
                "evt-rfq-awarded-001");
    }

    private static final class FakeInvoiceRepository implements InvoiceRepository {
        private final List<Invoice> invoices = new ArrayList<>();
        private final Map<UUID, UUID> approvalIdempotencyKeys = new LinkedHashMap<>();
        private final Map<UUID, UUID> disputeIdempotencyKeys = new LinkedHashMap<>();

        @Override
        public Optional<Invoice> findById(UUID invoiceId) {
            return invoices.stream().filter(invoice -> invoice.id().equals(invoiceId)).findFirst();
        }

        @Override
        public Optional<Invoice> findByIdempotencyKey(UUID idempotencyKey) {
            return invoices.stream().filter(invoice -> invoice.idempotencyKey().equals(idempotencyKey)).findFirst();
        }

        @Override
        public Optional<Invoice> findByIdAndMatchIdempotencyKey(UUID invoiceId, UUID idempotencyKey) {
            return invoices.stream()
                    .filter(invoice -> invoice.id().equals(invoiceId))
                    .filter(invoice -> idempotencyKey.equals(invoice.matchIdempotencyKey()))
                    .findFirst();
        }

        @Override
        public Optional<Invoice> findByIdAndApprovalIdempotencyKey(UUID invoiceId, UUID idempotencyKey) {
            if (!idempotencyKey.equals(approvalIdempotencyKeys.get(invoiceId))) {
                return Optional.empty();
            }
            return findById(invoiceId);
        }

        @Override
        public Optional<Invoice> findByIdAndDisputeIdempotencyKey(UUID invoiceId, UUID idempotencyKey) {
            if (!idempotencyKey.equals(disputeIdempotencyKeys.get(invoiceId))) {
                return Optional.empty();
            }
            return findById(invoiceId);
        }

        @Override
        public Optional<Invoice> findByVendorIdAndInvoiceNumber(UUID vendorId, String invoiceNumber) {
            return invoices.stream()
                    .filter(invoice -> invoice.vendorId().equals(vendorId))
                    .filter(invoice -> invoice.invoiceNumber().equals(invoiceNumber))
                    .findFirst();
        }

        @Override
        public List<Invoice> findByFilter(InvoiceFilter filter) {
            return invoices.stream()
                    .filter(invoice -> filter.status() == null || invoice.status() == filter.status())
                    .filter(invoice -> filter.vendorId() == null || invoice.vendorId().equals(filter.vendorId()))
                    .filter(invoice -> filter.poId() == null || invoice.poId().equals(filter.poId()))
                    .filter(invoice -> !Boolean.TRUE.equals(filter.overdueOnly())
                            || invoice.dueDate().isBefore(filter.overdueAsOf()))
                    .sorted(Comparator.comparing(Invoice::createdAt).reversed())
                    .skip(filter.offset())
                    .limit(filter.size())
                    .toList();
        }

        @Override
        public long countByFilter(InvoiceFilter filter) {
            return findByFilter(new InvoiceFilter(
                    filter.status(),
                    filter.vendorId(),
                    filter.poId(),
                    filter.overdueOnly(),
                    filter.overdueAsOf(),
                    1,
                    Integer.MAX_VALUE,
                    0)).size();
        }

        @Override
        public void insert(Invoice invoice) {
            invoices.add(invoice);
        }

        @Override
        public void updateMatchResult(
                UUID invoiceId,
                InvoiceStatus status,
                MatchStatus poMatchStatus,
                MatchStatus grMatchStatus,
                BigDecimal qtyVariance,
                Money priceVariance,
                Instant matchedAt,
                UUID matchedBy,
                UUID idempotencyKey) {
            Invoice invoice = findById(invoiceId).orElseThrow();
            replaceInvoice(new Invoice(
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.vendorId(),
                    invoice.vendorName(),
                    invoice.poId(),
                    invoice.poNumber(),
                    invoice.lineItems(),
                    invoice.subtotal(),
                    invoice.taxAmount(),
                    invoice.totalAmount(),
                    invoice.invoiceDate(),
                    invoice.dueDate(),
                    status,
                    poMatchStatus,
                    grMatchStatus,
                    qtyVariance,
                    priceVariance,
                    matchedAt,
                    matchedBy,
                    invoice.approvedBy(),
                    invoice.approvedAt(),
                    invoice.createdAt(),
                    invoice.createdBy(),
                    invoice.idempotencyKey(),
                    idempotencyKey));
        }

        @Override
        public void markApproved(UUID invoiceId, UUID approvedBy, Instant approvedAt, UUID idempotencyKey) {
            Invoice invoice = findById(invoiceId).orElseThrow();
            approvalIdempotencyKeys.put(invoiceId, idempotencyKey);
            replaceInvoice(new Invoice(
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.vendorId(),
                    invoice.vendorName(),
                    invoice.poId(),
                    invoice.poNumber(),
                    invoice.lineItems(),
                    invoice.subtotal(),
                    invoice.taxAmount(),
                    invoice.totalAmount(),
                    invoice.invoiceDate(),
                    invoice.dueDate(),
                    InvoiceStatus.APPROVED,
                    invoice.poMatchStatus(),
                    invoice.grMatchStatus(),
                    invoice.qtyVariance(),
                    invoice.priceVariance(),
                    invoice.matchedAt(),
                    invoice.matchedBy(),
                    approvedBy,
                    approvedAt,
                    invoice.createdAt(),
                    invoice.createdBy(),
                    invoice.idempotencyKey(),
                    invoice.matchIdempotencyKey()));
        }

        @Override
        public void markDisputed(UUID invoiceId, String reason, UUID disputedBy, Instant disputedAt, UUID idempotencyKey) {
            Invoice invoice = findById(invoiceId).orElseThrow();
            disputeIdempotencyKeys.put(invoiceId, idempotencyKey);
            replaceInvoice(new Invoice(
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.vendorId(),
                    invoice.vendorName(),
                    invoice.poId(),
                    invoice.poNumber(),
                    invoice.lineItems(),
                    invoice.subtotal(),
                    invoice.taxAmount(),
                    invoice.totalAmount(),
                    invoice.invoiceDate(),
                    invoice.dueDate(),
                    InvoiceStatus.DISPUTED,
                    invoice.poMatchStatus(),
                    invoice.grMatchStatus(),
                    invoice.qtyVariance(),
                    invoice.priceVariance(),
                    invoice.matchedAt(),
                    invoice.matchedBy(),
                    invoice.approvedBy(),
                    invoice.approvedAt(),
                    invoice.createdAt(),
                    invoice.createdBy(),
                    invoice.idempotencyKey(),
                    invoice.matchIdempotencyKey()));
        }

        @Override
        public boolean markPaid(UUID invoiceId, UUID paidBy, Instant paidAt) {
            Optional<Invoice> existing = findById(invoiceId);
            if (existing.isEmpty() || existing.get().status() != InvoiceStatus.APPROVED) {
                return false;
            }
            Invoice invoice = existing.get();
            replaceInvoice(new Invoice(
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.vendorId(),
                    invoice.vendorName(),
                    invoice.poId(),
                    invoice.poNumber(),
                    invoice.lineItems(),
                    invoice.subtotal(),
                    invoice.taxAmount(),
                    invoice.totalAmount(),
                    invoice.invoiceDate(),
                    invoice.dueDate(),
                    InvoiceStatus.PAID,
                    invoice.poMatchStatus(),
                    invoice.grMatchStatus(),
                    invoice.qtyVariance(),
                    invoice.priceVariance(),
                    invoice.matchedAt(),
                    invoice.matchedBy(),
                    invoice.approvedBy(),
                    invoice.approvedAt(),
                    invoice.createdAt(),
                    invoice.createdBy(),
                    invoice.idempotencyKey(),
                    invoice.matchIdempotencyKey()));
            return true;
        }

        private void replaceInvoice(Invoice replacement) {
            invoices.removeIf(invoice -> invoice.id().equals(replacement.id()));
            invoices.add(replacement);
        }
    }

    private static final class FakeGoodsReceiptSnapshotRepository implements GoodsReceiptSnapshotRepository {
        private final Map<UUID, BigDecimal> receivedQuantities = new LinkedHashMap<>();

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return false;
        }

        @Override
        public void upsert(com.eprocure.finance.domain.model.GoodsReceiptSnapshot snapshot) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public List<ReceivedQuantity> findReceivedQuantitiesByPoId(UUID poId) {
            return receivedQuantities.entrySet().stream()
                    .map(entry -> new ReceivedQuantity(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }

    private static final class FakeInvoiceMatchedEventPublisher implements InvoiceMatchedEventPublisher {
        private final List<InvoiceMatchedEvent> events = new ArrayList<>();

        @Override
        public void publish(InvoiceMatchedEvent event) {
            events.add(event);
        }
    }

    private static final class FakePaymentRepository implements PaymentRepository {
        private final List<Payment> payments = new ArrayList<>();

        @Override
        public Optional<Payment> findByIdempotencyKey(UUID idempotencyKey) {
            return payments.stream()
                    .filter(payment -> payment.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public Optional<Payment> findByInvoiceId(UUID invoiceId) {
            return payments.stream()
                    .filter(payment -> payment.invoiceId().equals(invoiceId))
                    .findFirst();
        }

        @Override
        public void insert(Payment payment) {
            payments.add(payment);
        }
    }

    private static final class FakePurchaseOrderRepository implements PurchaseOrderRepository {
        private final List<PurchaseOrder> purchaseOrders = new ArrayList<>();

        @Override
        public Optional<PurchaseOrder> findById(UUID poId) {
            return purchaseOrders.stream().filter(purchaseOrder -> purchaseOrder.id().equals(poId)).findFirst();
        }

        @Override
        public Optional<PurchaseOrder> findBySourceEventId(String sourceEventId) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public Optional<PurchaseOrder> findByRfqId(UUID rfqId) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public Optional<PurchaseOrder> findActiveManualByPrId(UUID prId) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public List<PurchaseOrder> findByFilter(PurchaseOrderFilter filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public long countByFilter(PurchaseOrderFilter filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public String nextPoNumber(int fiscalYear) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void insert(PurchaseOrder purchaseOrder) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void updateDraftDetails(PurchaseOrder purchaseOrder, UUID actorId) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void updateActionState(PurchaseOrder purchaseOrder, UUID actorId) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            throw new UnsupportedOperationException("not used");
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
            assertThat(idempotencyKey).isNotBlank();
        }
    }

    private static final class FakeBudgetDashboardCachePort implements BudgetDashboardCachePort {
        private final List<UUID> evictedBudgetIds = new ArrayList<>();

        @Override
        public Optional<BudgetDashboardView> findByBudgetId(UUID budgetId) {
            return Optional.empty();
        }

        @Override
        public void store(BudgetDashboardView dashboard) {
        }

        @Override
        public void evict(UUID budgetId) {
            evictedBudgetIds.add(budgetId);
        }
    }

    private static final class FakeBudgetRepository implements BudgetRepository {
        private final List<BudgetTransaction> transactions = new ArrayList<>();
        private BudgetCommitmentHold hold;
        private boolean locked = false;

        @Override
        public Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria) {
            return Optional.empty();
        }

        @Override
        public List<BudgetLedgerSummary> findByFilter(BudgetFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(BudgetFilter filter) {
            return 0;
        }

        @Override
        public Optional<BudgetLedgerSummary> findSummaryById(UUID budgetId) {
            return Optional.empty();
        }

        @Override
        public boolean lockBudgetForUpdate(UUID budgetId) {
            this.locked = true;
            return true;
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return false;
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        }

        @Override
        public boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId) {
            return transactions.stream()
                    .anyMatch(tx -> tx.budgetId().equals(budgetId)
                            && tx.transactionType() == transactionType
                            && tx.referenceType().equals(referenceType)
                            && tx.referenceId().equals(referenceId));
        }

        @Override
        public void insertTransaction(BudgetTransaction transaction) {
            transactions.add(transaction);
        }

        @Override
        public Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId) {
            return Optional.ofNullable(hold);
        }

        @Override
        public Optional<BudgetOverrideApproval> findOverrideApprovalByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insertOverrideApproval(BudgetOverrideApproval approval) {
        }

        @Override
        public Optional<BudgetTransfer> findTransferByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insertTransfer(BudgetTransfer transfer) {
        }

        @Override
        public void adjustAllocatedAmount(UUID budgetId, Money delta, UUID actorId) {
        }
    }
}
