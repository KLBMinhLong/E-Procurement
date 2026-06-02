package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.ApproveVendorCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.VendorDetailView;
import com.eprocure.vendor.application.service.VendorMutationResult;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveVendorUseCase {
    private static final Logger log = LogManager.getLogger(ApproveVendorUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "vendor-approve";

    private final VendorRepository vendorRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public ApproveVendorUseCase(
            VendorRepository vendorRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.vendorRepository = vendorRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public VendorMutationResult execute(ApproveVendorCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                VendorDetailView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit ApproveVendor | vendorId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.vendorId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return VendorMutationResult.replayed(cached.get());
        }

        Vendor vendor = vendorRepository.findById(command.vendorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_001));
        if (vendor.status() == VendorStatus.BLACKLISTED) {
            throw new BusinessException(ErrorCode.VND_003);
        }

        log.info("[ACTION] Start ApproveVendor | vendorId={} | userId={}",
                LogMaskingUtil.maskId(command.vendorId()),
                LogMaskingUtil.maskId(command.actorId()));
        Vendor approved = vendor.approve(command.actorId(), Instant.now(clock));
        if (vendor.status() != VendorStatus.APPROVED || !vendor.onApprovedVendorList()) {
            vendorRepository.updateApproval(approved);
        }
        VendorDetailView view = VendorDetailView.from(vendorRepository.findById(approved.id()).orElse(approved));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete ApproveVendor | vendorId={}", LogMaskingUtil.maskId(approved.id()));
        return VendorMutationResult.fresh(view);
    }
}
