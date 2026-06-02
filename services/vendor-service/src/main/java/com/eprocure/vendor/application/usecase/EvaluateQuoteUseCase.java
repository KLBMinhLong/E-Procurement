package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.EvaluateQuoteCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.VendorQuoteMutationResult;
import com.eprocure.vendor.application.service.VendorQuoteView;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.domain.model.VendorQuote;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorQuoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluateQuoteUseCase {
    private static final Logger log = LogManager.getLogger(EvaluateQuoteUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "vendor-quote-evaluate";

    private final RfqRepository rfqRepository;
    private final VendorQuoteRepository quoteRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public EvaluateQuoteUseCase(
            RfqRepository rfqRepository,
            VendorQuoteRepository quoteRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.rfqRepository = rfqRepository;
        this.quoteRepository = quoteRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public VendorQuoteMutationResult execute(EvaluateQuoteCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                VendorQuoteView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit EvaluateQuote | quoteId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.quoteId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return VendorQuoteMutationResult.replayed(cached.get());
        }

        Rfq rfq = rfqRepository.findById(command.rfqId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_004));
        if (rfq.status() == RfqStatus.AWARDED || rfq.status() == RfqStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.VND_005);
        }
        VendorQuote quote = quoteRepository.findById(command.quoteId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_006));
        if (!quote.rfqId().equals(rfq.id())) {
            throw new BusinessException(ErrorCode.VND_006);
        }

        log.info("[ACTION] Start EvaluateQuote | rfqId={} | quoteId={} | userId={}",
                LogMaskingUtil.maskId(rfq.id()),
                LogMaskingUtil.maskId(quote.id()),
                LogMaskingUtil.maskId(command.actorId()));
        VendorQuote evaluated = quote.evaluate(
                command.evaluationScore(),
                command.evaluationNote(),
                command.actorId(),
                Instant.now(clock));
        quoteRepository.updateEvaluation(evaluated);

        VendorQuoteView view = VendorQuoteView.from(quoteRepository.findById(evaluated.id()).orElse(evaluated));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete EvaluateQuote | quoteId={}", LogMaskingUtil.maskId(evaluated.id()));
        return VendorQuoteMutationResult.fresh(view);
    }
}
