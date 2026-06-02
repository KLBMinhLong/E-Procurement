package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.AwardRfqCommand;
import com.eprocure.vendor.application.port.out.RfqAwardedEventPublisher;
import com.eprocure.vendor.application.service.AwardRfqResult;
import com.eprocure.vendor.application.service.AwardRfqResult.AwardedVendorView;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.VendorQuoteView;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorQuote;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.event.RfqAwardedEvent;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorQuoteRepository;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AwardRfqUseCase {
    private static final Logger log = LogManager.getLogger(AwardRfqUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "rfq-award";

    private final RfqRepository rfqRepository;
    private final VendorQuoteRepository quoteRepository;
    private final VendorRepository vendorRepository;
    private final IdempotencyService idempotencyService;
    private final RfqAwardedEventPublisher eventPublisher;
    private final Clock clock;

    public AwardRfqUseCase(
            RfqRepository rfqRepository,
            VendorQuoteRepository quoteRepository,
            VendorRepository vendorRepository,
            IdempotencyService idempotencyService,
            RfqAwardedEventPublisher eventPublisher,
            Clock clock) {
        this.rfqRepository = rfqRepository;
        this.quoteRepository = quoteRepository;
        this.vendorRepository = vendorRepository;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public AwardRfqResult execute(AwardRfqCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                AwardRfqResult.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit AwardRfq | rfqId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.rfqId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return AwardRfqResult.replayed(cached.get().awardedVendor(), cached.get().awardedQuote());
        }

        Rfq rfq = rfqRepository.findById(command.rfqId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_004));
        VendorQuote quote = quoteRepository.findById(command.awardedQuoteId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_006));
        if (!quote.rfqId().equals(rfq.id())) {
            throw new BusinessException(ErrorCode.VND_006);
        }
        Vendor vendor = vendorRepository.findById(quote.vendorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_001));
        ensureAwardableVendor(vendor);
        ensureAwardReason(command.awardReason());

        log.info("[ACTION] Start AwardRfq | rfqId={} | quoteId={} | userId={}",
                LogMaskingUtil.maskId(rfq.id()),
                LogMaskingUtil.maskId(quote.id()),
                LogMaskingUtil.maskId(command.actorId()));
        Instant now = Instant.now(clock);
        Rfq awarded = award(rfq, quote, command, now);
        rfqRepository.updateStatus(awarded);
        eventPublisher.publish(RfqAwardedEvent.create(awarded, quote, vendor, command.actorId(), now));

        AwardRfqResult result = AwardRfqResult.fresh(
                new AwardedVendorView(vendor.id(), vendor.name()),
                VendorQuoteView.from(quoteRepository.findById(quote.id()).orElse(quote)));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, result);
        log.info("[ACTION] Complete AwardRfq | rfqId={} | awardedQuoteId={}",
                LogMaskingUtil.maskId(rfq.id()),
                LogMaskingUtil.maskId(quote.id()));
        return result;
    }

    private void ensureAwardableVendor(Vendor vendor) {
        if (vendor.status() == VendorStatus.BLACKLISTED) {
            throw new BusinessException(ErrorCode.VND_003);
        }
        if (vendor.status() != VendorStatus.APPROVED || !vendor.onApprovedVendorList()) {
            throw new BusinessException(ErrorCode.VND_008);
        }
    }

    private void ensureAwardReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() < 20) {
            throw new BusinessException(ErrorCode.VND_011);
        }
    }

    private Rfq award(Rfq rfq, VendorQuote quote, AwardRfqCommand command, Instant now) {
        try {
            return rfq.award(quote.id(), quote.vendorId(), command.awardReason(), command.actorId(), now);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.VND_005);
        }
    }
}
