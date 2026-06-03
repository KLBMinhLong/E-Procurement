package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ApproveInvoiceCommand;
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
public class ApproveInvoiceUseCase {
    private static final Logger log = LogManager.getLogger(ApproveInvoiceUseCase.class);

    private final InvoiceRepository invoiceRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public ApproveInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public InvoiceActionResult execute(ApproveInvoiceCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = invoiceRepository.findByIdAndApprovalIdempotencyKey(command.invoiceId(), key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit ApproveInvoice | invoiceId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.invoiceId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return new InvoiceActionResult(command.invoiceId(), replayed.get().status(), true);
        }

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_007));
        if (invoice.status() != InvoiceStatus.MATCHED) {
            throw new BusinessException(ErrorCode.FIN_013);
        }
        Instant approvedAt = Instant.now(clock);
        log.info("[ACTION] Start ApproveInvoice | invoiceId={} | userId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(command.actorId()));
        invoiceRepository.markApproved(invoice.id(), command.actorId(), approvedAt, key);
        log.info("[ACTION] Complete ApproveInvoice | invoiceId={} | userId={}",
                LogMaskingUtil.maskId(invoice.id()),
                LogMaskingUtil.maskId(command.actorId()));
        return new InvoiceActionResult(invoice.id(), InvoiceStatus.APPROVED, false);
    }
}
