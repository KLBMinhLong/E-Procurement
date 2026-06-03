package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.DisputeInvoiceCommand;
import com.eprocure.finance.application.service.IdempotencyService;
import com.eprocure.finance.application.service.InvoiceActionResult;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisputeInvoiceUseCase {
    private static final Logger log = LogManager.getLogger(DisputeInvoiceUseCase.class);

    private final InvoiceRepository invoiceRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public DisputeInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public InvoiceActionResult execute(DisputeInvoiceCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = invoiceRepository.findByIdAndDisputeIdempotencyKey(command.invoiceId(), key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit DisputeInvoice | invoiceId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.invoiceId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return new InvoiceActionResult(command.invoiceId(), replayed.get().status(), true);
        }

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_007));
        if (invoice.status() != InvoiceStatus.MISMATCHED) {
            throw new BusinessException(ErrorCode.FIN_014);
        }
        Instant disputedAt = Instant.now(clock);
        log.info("[ACTION] Start DisputeInvoice | invoiceId={} | userId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(command.actorId()));
        invoiceRepository.markDisputed(invoice.id(), command.reason(), command.actorId(), disputedAt, key);
        log.info("[ACTION] Complete DisputeInvoice | invoiceId={} | userId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(command.actorId()));
        return new InvoiceActionResult(invoice.id(), InvoiceStatus.DISPUTED, false);
    }
}
