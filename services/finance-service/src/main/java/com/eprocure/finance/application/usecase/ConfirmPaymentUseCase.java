package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ConfirmPaymentCommand;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.PaymentConfirmationResult;
import com.eprocure.finance.application.service.PaymentView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.Payment;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.BudgetRepository;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import com.eprocure.finance.domain.repository.PaymentRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmPaymentUseCase {
    private static final Logger log = LogManager.getLogger(ConfirmPaymentUseCase.class);
    private static final String PURCHASE_REQUEST_REFERENCE = "PURCHASE_REQUEST";
    private static final String PURCHASE_ORDER_REFERENCE = "PURCHASE_ORDER";
    private static final String INVOICE_REFERENCE = "INVOICE";

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetDashboardCachePort budgetDashboardCachePort;

    public ConfirmPaymentUseCase(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            IdempotencyService idempotencyService,
            Clock clock,
            PurchaseOrderRepository purchaseOrderRepository,
            BudgetRepository budgetRepository,
            BudgetDashboardCachePort budgetDashboardCachePort) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.budgetRepository = budgetRepository;
        this.budgetDashboardCachePort = budgetDashboardCachePort;
    }

    @Transactional
    public PaymentConfirmationResult execute(ConfirmPaymentCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = paymentRepository.findByIdempotencyKey(key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit ConfirmPayment | invoiceId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(replayed.get().invoiceId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return new PaymentConfirmationResult(PaymentView.from(replayed.get()), true);
        }

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_007));
        if (invoice.status() != InvoiceStatus.APPROVED) {
            throw new BusinessException(ErrorCode.FIN_015);
        }
        BigDecimal amount = command.paidAmount() == null
                ? invoice.totalAmount().amount()
                : command.paidAmount();
        Money paidAmount = new Money(amount, invoice.totalAmount().currency());
        if (paidAmount.compareTo(invoice.totalAmount()) != 0) {
            throw new BusinessException(ErrorCode.FIN_016);
        }

        Instant confirmedAt = Instant.now(clock);
        PurchaseOrder po = purchaseOrderRepository.findById(invoice.poId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));

        log.info("[ACTION] Start ConfirmPayment | invoiceId={} | userId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(command.actorId()));
        if (!invoiceRepository.markPaid(invoice.id(), command.actorId(), confirmedAt)) {
            throw new BusinessException(ErrorCode.FIN_015);
        }

        var holdOpt = budgetRepository.findHeldCommitment(PURCHASE_REQUEST_REFERENCE, po.prId());
        if (holdOpt.isPresent()) {
            BudgetCommitmentHold hold = holdOpt.get();
            UUID budgetId = hold.budgetId();

            if (!budgetRepository.lockBudgetForUpdate(budgetId)) {
                throw new BusinessException(ErrorCode.FIN_001);
            }

            if (hold.hasHeldAmount()) {
                BudgetTransaction releaseTx = new BudgetTransaction(
                        budgetId,
                        BudgetTransactionType.RELEASE,
                        hold.amount(),
                        PURCHASE_ORDER_REFERENCE,
                        po.id(),
                        "Release commitment hold for Purchase Order payment: " + po.poNumber(),
                        command.actorId(),
                        confirmedAt,
                        null
                );
                budgetRepository.insertTransaction(releaseTx);
            }

            BudgetTransaction spendTx = new BudgetTransaction(
                    budgetId,
                    BudgetTransactionType.SPEND,
                    paidAmount,
                    INVOICE_REFERENCE,
                    invoice.id(),
                    "Invoice payment confirmation: " + invoice.invoiceNumber(),
                    command.actorId(),
                    confirmedAt,
                    null
            );
            budgetRepository.insertTransaction(spendTx);
            budgetDashboardCachePort.evict(budgetId);
        } else {
            log.warn("[ACTION] No budget commitment hold found for PURCHASE_REQUEST | prId={} | poId={}",
                    LogMaskingUtil.maskId(po.prId()),
                    LogMaskingUtil.maskId(po.id()));
        }

        Payment payment = Payment.confirm(
                invoice.id(),
                command.paymentDate(),
                command.paymentReference(),
                paidAmount,
                command.notes(),
                key,
                confirmedAt,
                command.actorId());
        paymentRepository.insert(payment);
        log.info("[ACTION] Complete ConfirmPayment | invoiceId={} | paymentId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(payment.id()));
        return new PaymentConfirmationResult(PaymentView.from(payment), false);
    }
}
