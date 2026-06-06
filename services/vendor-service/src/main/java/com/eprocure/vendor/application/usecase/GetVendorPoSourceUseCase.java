package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.service.VendorPoSourceView;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetVendorPoSourceUseCase {
    private static final Logger log = LogManager.getLogger(GetVendorPoSourceUseCase.class);

    private final VendorRepository vendorRepository;

    public GetVendorPoSourceUseCase(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public VendorPoSourceView execute(UUID vendorId) {
        Objects.requireNonNull(vendorId, "vendorId must not be null");
        log.info("[ACTION] Start GetVendorPoSource | vendorId={}", LogMaskingUtil.maskId(vendorId));

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_001));
        if (vendor.status() != VendorStatus.APPROVED || !vendor.onApprovedVendorList()) {
            throw new BusinessException(ErrorCode.VND_008);
        }

        log.info("[ACTION] Complete GetVendorPoSource | vendorId={} | status={}",
                LogMaskingUtil.maskId(vendor.id()),
                vendor.status());
        return VendorPoSourceView.from(vendor);
    }
}
