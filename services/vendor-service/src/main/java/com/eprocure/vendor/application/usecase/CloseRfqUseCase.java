package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.CloseRfqCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.RfqDetailView;
import com.eprocure.vendor.application.service.RfqMutationResult;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.repository.RfqRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseRfqUseCase {
    private static final Logger log = LogManager.getLogger(CloseRfqUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "rfq-close";

    private final RfqRepository rfqRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CloseRfqUseCase(
            RfqRepository rfqRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.rfqRepository = rfqRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public RfqMutationResult execute(CloseRfqCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                RfqDetailView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CloseRfq | rfqId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.rfqId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return RfqMutationResult.replayed(cached.get());
        }

        Rfq rfq = rfqRepository.findById(command.rfqId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_004));
        log.info("[ACTION] Start CloseRfq | rfqId={} | userId={}",
                LogMaskingUtil.maskId(command.rfqId()),
                LogMaskingUtil.maskId(command.actorId()));
        Rfq closed = close(rfq, command, Instant.now(clock));
        if (closed != rfq) {
            rfqRepository.updateStatus(closed);
        }
        RfqDetailView view = RfqDetailView.from(rfqRepository.findById(closed.id()).orElse(closed));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete CloseRfq | rfqId={}", LogMaskingUtil.maskId(closed.id()));
        return RfqMutationResult.fresh(view);
    }

    private Rfq close(Rfq rfq, CloseRfqCommand command, Instant closedAt) {
        try {
            return rfq.close(command.actorId(), closedAt);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.VND_005);
        }
    }
}
