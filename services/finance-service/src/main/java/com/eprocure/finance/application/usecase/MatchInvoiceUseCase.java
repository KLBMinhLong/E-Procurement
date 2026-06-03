package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.MatchInvoiceCommand;
import com.eprocure.finance.application.port.out.InvoiceMatchedEventPublisher;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.InvoiceMatchResult;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.event.InvoiceMatchedEvent;
import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceLineItem;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.GoodsReceiptSnapshotRepository;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchInvoiceUseCase {
    private static final Logger log = LogManager.getLogger(MatchInvoiceUseCase.class);

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptSnapshotRepository goodsReceiptSnapshotRepository;
    private final InvoiceMatchedEventPublisher invoiceMatchedEventPublisher;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public MatchInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            GoodsReceiptSnapshotRepository goodsReceiptSnapshotRepository,
            InvoiceMatchedEventPublisher invoiceMatchedEventPublisher,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptSnapshotRepository = goodsReceiptSnapshotRepository;
        this.invoiceMatchedEventPublisher = invoiceMatchedEventPublisher;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public InvoiceMatchResult execute(MatchInvoiceCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = invoiceRepository.findByIdAndMatchIdempotencyKey(command.invoiceId(), key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit MatchInvoice | invoiceId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.invoiceId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return toResult(replayed.get(), true);
        }

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_007));
        if (invoice.status() == InvoiceStatus.APPROVED
                || invoice.status() == InvoiceStatus.PAID
                || invoice.status() == InvoiceStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.FIN_012);
        }
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(invoice.poId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));

        log.info("[ACTION] Start MatchInvoice | invoiceId={} | poId={} | userId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(invoice.poId()),
                LogMaskingUtil.maskId(command.actorId()));

        MatchComputation computation = compute(invoice, purchaseOrder);
        Instant matchedAt = Instant.now(clock);
        InvoiceStatus nextStatus = computation.overallStatus() == MatchStatus.MATCHED
                ? InvoiceStatus.MATCHED
                : InvoiceStatus.MISMATCHED;
        invoiceRepository.updateMatchResult(
                invoice.id(),
                nextStatus,
                computation.poMatchStatus(),
                computation.grMatchStatus(),
                computation.qtyVariance(),
                computation.priceVariance(),
                matchedAt,
                command.actorId(),
                key);
        Invoice updated = invoiceRepository.findById(invoice.id()).orElseThrow(() -> new BusinessException(ErrorCode.FIN_007));
        if (computation.overallStatus() == MatchStatus.MATCHED) {
            invoiceMatchedEventPublisher.publish(InvoiceMatchedEvent.create(updated, matchedAt));
        }

        log.info("[ACTION] Complete MatchInvoice | invoiceId={} | matchStatus={} | requiresManualReview={}",
                LogMaskingUtil.maskId(invoice.id()),
                computation.overallStatus(),
                computation.requiresManualReview());
        return new InvoiceMatchResult(
                computation.overallStatus(),
                computation.poMatchStatus(),
                computation.grMatchStatus(),
                computation.qtyVariance(),
                computation.priceVariance(),
                matchedAt,
                computation.requiresManualReview(),
                false);
    }

    private MatchComputation compute(Invoice invoice, PurchaseOrder purchaseOrder) {
        Map<UUID, PurchaseOrderLineItem> poLines = purchaseOrder.lineItems().stream()
                .collect(Collectors.toMap(PurchaseOrderLineItem::id, Function.identity()));
        Map<UUID, BigDecimal> receivedQuantities = goodsReceiptSnapshotRepository.findReceivedQuantitiesByPoId(invoice.poId())
                .stream()
                .collect(Collectors.toMap(
                        GoodsReceiptSnapshotRepository.ReceivedQuantity::poLineItemId,
                        GoodsReceiptSnapshotRepository.ReceivedQuantity::receivedQuantity));

        MatchStatus poStatus = poMatchStatus(invoice, purchaseOrder, poLines);
        MatchStatus grStatus = grMatchStatus(invoice, receivedQuantities);
        BigDecimal invoiceQuantity = invoice.lineItems().stream()
                .map(InvoiceLineItem::quantity)
                .reduce(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal receivedQuantity = invoice.lineItems().stream()
                .map(line -> receivedQuantities.getOrDefault(line.poLineItemId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal qtyVariance = invoiceQuantity.subtract(receivedQuantity).setScale(4, RoundingMode.HALF_UP);
        Money priceVariance = new Money(
                invoice.subtotal().amount().subtract(purchaseOrder.totalAmount().amount()).setScale(4, RoundingMode.HALF_UP),
                invoice.subtotal().currency());
        MatchStatus overallStatus = overallStatus(poStatus, grStatus);
        return new MatchComputation(
                overallStatus,
                poStatus,
                grStatus,
                qtyVariance,
                priceVariance,
                overallStatus != MatchStatus.MATCHED);
    }

    private MatchStatus poMatchStatus(
            Invoice invoice,
            PurchaseOrder purchaseOrder,
            Map<UUID, PurchaseOrderLineItem> poLines) {
        boolean exact = invoice.subtotal().compareTo(purchaseOrder.totalAmount()) == 0
                && invoice.lineItems().size() == purchaseOrder.lineItems().size();
        for (InvoiceLineItem invoiceLine : invoice.lineItems()) {
            PurchaseOrderLineItem poLine = poLines.get(invoiceLine.poLineItemId());
            if (poLine == null
                    || invoiceLine.quantity().compareTo(poLine.quantity()) > 0
                    || invoiceLine.unitPrice().compareTo(poLine.unitPrice()) != 0) {
                return MatchStatus.MISMATCHED;
            }
            exact = exact
                    && invoiceLine.quantity().compareTo(poLine.quantity()) == 0
                    && invoiceLine.totalPrice().compareTo(poLine.totalPrice()) == 0;
        }
        return exact ? MatchStatus.MATCHED : MatchStatus.PARTIAL;
    }

    private MatchStatus grMatchStatus(Invoice invoice, Map<UUID, BigDecimal> receivedQuantities) {
        boolean exact = true;
        for (InvoiceLineItem invoiceLine : invoice.lineItems()) {
            BigDecimal receivedQuantity = receivedQuantities.get(invoiceLine.poLineItemId());
            if (receivedQuantity == null || invoiceLine.quantity().compareTo(receivedQuantity) > 0) {
                return MatchStatus.MISMATCHED;
            }
            exact = exact && invoiceLine.quantity().compareTo(receivedQuantity) == 0;
        }
        return exact ? MatchStatus.MATCHED : MatchStatus.PARTIAL;
    }

    private MatchStatus overallStatus(MatchStatus poStatus, MatchStatus grStatus) {
        if (poStatus == MatchStatus.MISMATCHED || grStatus == MatchStatus.MISMATCHED) {
            return MatchStatus.MISMATCHED;
        }
        if (poStatus == MatchStatus.MATCHED && grStatus == MatchStatus.MATCHED) {
            return MatchStatus.MATCHED;
        }
        return MatchStatus.PARTIAL;
    }

    private InvoiceMatchResult toResult(Invoice invoice, boolean replayed) {
        MatchStatus overallStatus = overallStatus(invoice.poMatchStatus(), invoice.grMatchStatus());
        return new InvoiceMatchResult(
                overallStatus,
                invoice.poMatchStatus(),
                invoice.grMatchStatus(),
                invoice.qtyVariance(),
                invoice.priceVariance(),
                invoice.matchedAt(),
                overallStatus != MatchStatus.MATCHED,
                replayed);
    }

    private record MatchComputation(
            MatchStatus overallStatus,
            MatchStatus poMatchStatus,
            MatchStatus grMatchStatus,
            BigDecimal qtyVariance,
            Money priceVariance,
            boolean requiresManualReview) {
    }
}
