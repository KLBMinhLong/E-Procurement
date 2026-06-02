package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.service.RfqDetailView;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorQuoteRepository;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRfqDetailUseCase {
    private static final Logger log = LogManager.getLogger(GetRfqDetailUseCase.class);

    private final RfqRepository rfqRepository;
    private final VendorQuoteRepository quoteRepository;

    public GetRfqDetailUseCase(RfqRepository rfqRepository, VendorQuoteRepository quoteRepository) {
        this.rfqRepository = rfqRepository;
        this.quoteRepository = quoteRepository;
    }

    @Transactional(readOnly = true)
    public RfqDetailView execute(UUID rfqId, UUID actorId) {
        Objects.requireNonNull(rfqId, "rfqId must not be null");
        log.info("[ACTION] Start GetRfqDetail | rfqId={} | userId={}",
                LogMaskingUtil.maskId(rfqId),
                LogMaskingUtil.maskId(actorId));
        return rfqRepository.findById(rfqId)
                .map(rfq -> RfqDetailView.from(rfq, quoteRepository.findByRfqId(rfq.id())))
                .orElseThrow(() -> new BusinessException(ErrorCode.VND_004));
    }
}
