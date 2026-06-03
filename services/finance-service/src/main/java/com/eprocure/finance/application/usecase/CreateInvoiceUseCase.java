package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.CreateInvoiceCommand;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.InvoiceMutationResult;
import com.eprocure.finance.application.service.InvoiceView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceLineItem;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateInvoiceUseCase {
    private static final Logger log = LogManager.getLogger(CreateInvoiceUseCase.class);

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreateInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public InvoiceMutationResult execute(CreateInvoiceCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = invoiceRepository.findByIdempotencyKey(key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateInvoice | invoiceId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(replayed.get().id()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return InvoiceMutationResult.replayed(InvoiceView.from(replayed.get()));
        }
        if (command.dueDate().isBefore(command.invoiceDate())) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        if (invoiceRepository.findByVendorIdAndInvoiceNumber(command.vendorId(), command.invoiceNumber()).isPresent()) {
            throw new BusinessException(ErrorCode.FIN_008);
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(command.poId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));
        if (!purchaseOrder.vendorId().equals(command.vendorId())) {
            throw new BusinessException(ErrorCode.FIN_010);
        }

        AtomicInteger lineNumber = new AtomicInteger(1);
        var lineItems = command.lineItems().stream()
                .map(item -> InvoiceLineItem.create(
                        lineNumber.getAndIncrement(),
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.taxRate(),
                        purchaseOrder.totalAmount().currency()))
                .toList();
        Invoice invoice = Invoice.create(
                command.invoiceNumber(),
                command.vendorId(),
                purchaseOrder.vendorName(),
                purchaseOrder.id(),
                purchaseOrder.poNumber(),
                lineItems,
                command.invoiceDate(),
                command.dueDate(),
                Instant.now(clock),
                command.actorId(),
                key,
                purchaseOrder.totalAmount().currency());

        log.info("[ACTION] Start CreateInvoice | invoiceNumber={} | poId={} | userId={}",
                invoice.invoiceNumber(),
                LogMaskingUtil.maskId(invoice.poId()),
                LogMaskingUtil.maskId(command.actorId()));
        invoiceRepository.insert(invoice);
        log.info("[ACTION] Complete CreateInvoice | invoiceId={} | invoiceNumber={}",
                LogMaskingUtil.maskId(invoice.id()),
                invoice.invoiceNumber());
        return InvoiceMutationResult.fresh(InvoiceView.from(invoice));
    }
}
