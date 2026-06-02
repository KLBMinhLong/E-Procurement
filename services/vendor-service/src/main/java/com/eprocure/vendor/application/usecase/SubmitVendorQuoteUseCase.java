package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.SubmitVendorQuoteCommand;
import com.eprocure.vendor.application.port.in.SubmitVendorQuoteCommand.SubmitVendorQuoteLineItemCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.VendorQuoteMutationResult;
import com.eprocure.vendor.application.service.VendorQuoteView;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqLineItem;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorQuote;
import com.eprocure.vendor.domain.model.VendorQuoteLineItem;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorQuoteRepository;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
public class SubmitVendorQuoteUseCase {
    private static final Logger log = LogManager.getLogger(SubmitVendorQuoteUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "vendor-quote-submit";

    private final RfqRepository rfqRepository;
    private final VendorRepository vendorRepository;
    private final VendorQuoteRepository quoteRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public SubmitVendorQuoteUseCase(
            RfqRepository rfqRepository,
            VendorRepository vendorRepository,
            VendorQuoteRepository quoteRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.rfqRepository = rfqRepository;
        this.vendorRepository = vendorRepository;
        this.quoteRepository = quoteRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public VendorQuoteMutationResult execute(SubmitVendorQuoteCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                VendorQuoteView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit SubmitVendorQuote | rfqId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.rfqId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return VendorQuoteMutationResult.replayed(cached.get());
        }

        UUID key = UUID.fromString(idempotencyKey);
        var existingByKey = quoteRepository.findByIdempotencyKey(key);
        if (existingByKey.isPresent()) {
            VendorQuoteView view = VendorQuoteView.from(existingByKey.get());
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return VendorQuoteMutationResult.replayed(view);
        }

        Instant now = Instant.now(clock);
        Rfq rfq = rfqRepository.findById(command.rfqId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_004));
        ensureQuoteWindowOpen(rfq, now);
        Vendor vendor = loadAllowedVendor(command.vendorId());
        ensureVendorInvited(rfq, vendor.id());
        quoteRepository.findByRfqIdAndVendorId(rfq.id(), vendor.id())
                .ifPresent(quote -> {
                    throw new BusinessException(ErrorCode.VND_010);
                });

        List<VendorQuoteLineItem> lineItems = toQuoteLineItems(rfq, command);
        LocalDate validUntil = Objects.requireNonNull(command.validUntil(), "validUntil must not be null");
        if (validUntil.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.VND_011);
        }

        log.info("[ACTION] Start SubmitVendorQuote | rfqId={} | vendorId={} | userId={}",
                LogMaskingUtil.maskId(rfq.id()),
                LogMaskingUtil.maskId(vendor.id()),
                LogMaskingUtil.maskId(command.actorId()));
        VendorQuote quote = VendorQuote.create(
                UUID.randomUUID(),
                rfq.id(),
                vendor.id(),
                vendor.name(),
                lineItems,
                command.currency(),
                validUntil,
                command.paymentTerms(),
                command.notes(),
                command.actorId(),
                key,
                now);
        quoteRepository.save(quote);
        rfqRepository.markInvitationSubmitted(rfq.id(), vendor.id(), now, command.actorId());

        VendorQuoteView view = VendorQuoteView.from(quoteRepository.findById(quote.id()).orElse(quote));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete SubmitVendorQuote | quoteId={} | rfqId={}",
                LogMaskingUtil.maskId(quote.id()),
                LogMaskingUtil.maskId(rfq.id()));
        return VendorQuoteMutationResult.fresh(view);
    }

    private void ensureQuoteWindowOpen(Rfq rfq, Instant now) {
        if (rfq.status() != RfqStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.VND_005);
        }
        if (!now.isBefore(rfq.submissionDeadline())) {
            throw new BusinessException(ErrorCode.VND_007);
        }
    }

    private Vendor loadAllowedVendor(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_001));
        if (vendor.status() == VendorStatus.BLACKLISTED) {
            throw new BusinessException(ErrorCode.VND_003);
        }
        if (vendor.status() != VendorStatus.APPROVED || !vendor.onApprovedVendorList()) {
            throw new BusinessException(ErrorCode.VND_008);
        }
        return vendor;
    }

    private void ensureVendorInvited(Rfq rfq, UUID vendorId) {
        boolean invited = rfq.invitations().stream()
                .anyMatch(invitation -> invitation.vendorId().equals(vendorId));
        if (!invited) {
            throw new BusinessException(ErrorCode.VND_008);
        }
    }

    private List<VendorQuoteLineItem> toQuoteLineItems(Rfq rfq, SubmitVendorQuoteCommand command) {
        Map<UUID, RfqLineItem> rfqItems = rfq.lineItems().stream()
                .collect(Collectors.toMap(RfqLineItem::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<UUID, SubmitVendorQuoteLineItemCommand> submittedItems = command.lineItems().stream()
                .collect(Collectors.toMap(
                        SubmitVendorQuoteLineItemCommand::rfqLineItemId,
                        Function.identity(),
                        (left, right) -> {
                            throw new BusinessException(ErrorCode.VND_011);
                        },
                        LinkedHashMap::new));
        if (!submittedItems.keySet().equals(rfqItems.keySet())) {
            throw new BusinessException(ErrorCode.VND_011);
        }
        return rfqItems.values().stream()
                .map(rfqItem -> toQuoteLineItem(rfqItem, submittedItems.get(rfqItem.id()), command.currency()))
                .toList();
    }

    private VendorQuoteLineItem toQuoteLineItem(
            RfqLineItem rfqItem,
            SubmitVendorQuoteLineItemCommand item,
            String currency) {
        return VendorQuoteLineItem.create(
                rfqItem.id(),
                rfqItem.itemName(),
                rfqItem.quantity(),
                item.unitPrice(),
                currency,
                item.deliveryDays(),
                item.warranty());
    }
}
