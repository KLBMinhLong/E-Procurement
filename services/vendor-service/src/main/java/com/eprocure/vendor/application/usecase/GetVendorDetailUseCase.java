package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.service.VendorDetailView;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetVendorDetailUseCase {
    private static final Logger log = LogManager.getLogger(GetVendorDetailUseCase.class);

    private final VendorRepository vendorRepository;

    public GetVendorDetailUseCase(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public VendorDetailView execute(UUID vendorId, UUID actorId) {
        Objects.requireNonNull(vendorId, "vendorId must not be null");
        log.info("[ACTION] Start GetVendorDetail | vendorId={} | userId={}",
                LogMaskingUtil.maskId(vendorId),
                LogMaskingUtil.maskId(actorId));
        return vendorRepository.findById(vendorId)
                .map(VendorDetailView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_001));
    }
}
