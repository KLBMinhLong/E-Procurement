package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.GetInvoiceQuery;
import com.eprocure.finance.application.service.InvoiceView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetInvoiceUseCase {
    private static final Logger log = LogManager.getLogger(GetInvoiceUseCase.class);

    private final InvoiceRepository invoiceRepository;

    public GetInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceView execute(GetInvoiceQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        log.info("[ACTION] Start GetInvoice | userId={} | invoiceId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.invoiceId()));
        return invoiceRepository.findById(query.invoiceId())
                .map(InvoiceView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_007));
    }
}
