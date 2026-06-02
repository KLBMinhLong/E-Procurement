package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.CreateRfqCommand;
import com.eprocure.vendor.application.port.out.PurchaseRequestRfqSourcePort;
import com.eprocure.vendor.application.port.out.PurchaseRequestRfqSourcePort.PurchaseRequestRfqLineItem;
import com.eprocure.vendor.application.port.out.PurchaseRequestRfqSourcePort.PurchaseRequestRfqSource;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.RfqDetailView;
import com.eprocure.vendor.application.service.RfqMutationResult;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqInvitation;
import com.eprocure.vendor.domain.model.RfqLineItem;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRfqUseCase {
    private static final Logger log = LogManager.getLogger(CreateRfqUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "rfq-create";
    private static final String APPROVED_PR_STATUS = "APPROVED";

    private final RfqRepository rfqRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseRequestRfqSourcePort purchaseRequestRfqSourcePort;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreateRfqUseCase(
            RfqRepository rfqRepository,
            VendorRepository vendorRepository,
            PurchaseRequestRfqSourcePort purchaseRequestRfqSourcePort,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.rfqRepository = rfqRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseRequestRfqSourcePort = purchaseRequestRfqSourcePort;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public RfqMutationResult execute(CreateRfqCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                RfqDetailView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateRfq | prId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.prId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return RfqMutationResult.replayed(cached.get());
        }

        UUID key = UUID.fromString(idempotencyKey);
        var existing = rfqRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            RfqDetailView view = RfqDetailView.from(existing.get());
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return RfqMutationResult.replayed(view);
        }

        Instant now = Instant.now(clock);
        PurchaseRequestRfqSource source = purchaseRequestRfqSourcePort.getSource(command.prId());
        validateSource(source);
        List<Vendor> vendors = loadApprovedVendors(command.invitedVendorIds());

        log.info("[ACTION] Start CreateRfq | prId={} | userId={} | vendorCount={}",
                LogMaskingUtil.maskId(command.prId()),
                LogMaskingUtil.maskId(command.actorId()),
                vendors.size());

        Rfq rfq = Rfq.create(
                UUID.randomUUID(),
                rfqRepository.nextRfqNumber(),
                source.id(),
                source.prNumber(),
                command.title(),
                command.submissionDeadline(),
                toLineItems(source.lineItems()),
                toInvitations(vendors, now),
                command.requirements(),
                command.actorId(),
                key,
                now);
        rfqRepository.save(rfq);

        RfqDetailView view = RfqDetailView.from(rfqRepository.findById(rfq.id()).orElse(rfq));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete CreateRfq | rfqId={} | rfqNumber={}",
                LogMaskingUtil.maskId(rfq.id()),
                rfq.rfqNumber());
        return RfqMutationResult.fresh(view);
    }

    private void validateSource(PurchaseRequestRfqSource source) {
        if (source == null || !APPROVED_PR_STATUS.equals(source.status())) {
            throw new BusinessException(ErrorCode.VND_009);
        }
        if (source.lineItems().isEmpty()) {
            throw new BusinessException(ErrorCode.VND_009);
        }
    }

    private List<Vendor> loadApprovedVendors(List<UUID> invitedVendorIds) {
        List<UUID> distinctIds = new LinkedHashSet<>(invitedVendorIds).stream().toList();
        if (distinctIds.size() < 2) {
            throw new BusinessException(ErrorCode.VND_008);
        }
        return distinctIds.stream()
                .map(id -> vendorRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VND_001)))
                .peek(this::ensureApprovedVendor)
                .toList();
    }

    private void ensureApprovedVendor(Vendor vendor) {
        if (vendor.status() == VendorStatus.BLACKLISTED) {
            throw new BusinessException(ErrorCode.VND_003);
        }
        if (vendor.status() != VendorStatus.APPROVED || !vendor.onApprovedVendorList()) {
            throw new BusinessException(ErrorCode.VND_008);
        }
    }

    private List<RfqLineItem> toLineItems(List<PurchaseRequestRfqLineItem> items) {
        return items.stream()
                .map(item -> RfqLineItem.create(
                        item.id(),
                        item.itemName(),
                        item.categoryCode(),
                        item.quantity(),
                        item.unit(),
                        item.specifications()))
                .toList();
    }

    private List<RfqInvitation> toInvitations(List<Vendor> vendors, Instant invitedAt) {
        return vendors.stream()
                .map(vendor -> RfqInvitation.create(vendor.id(), vendor.name(), invitedAt))
                .toList();
    }
}
