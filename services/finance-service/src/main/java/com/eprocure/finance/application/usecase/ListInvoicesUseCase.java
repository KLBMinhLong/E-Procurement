package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ListInvoicesQuery;
import com.eprocure.finance.application.service.InvoiceView;
import com.eprocure.finance.application.service.PageMeta;
import com.eprocure.finance.application.service.PageResult;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.repository.InvoiceFilter;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListInvoicesUseCase {
    private static final Logger log = LogManager.getLogger(ListInvoicesUseCase.class);
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    public ListInvoicesUseCase(InvoiceRepository invoiceRepository, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult<InvoiceView> execute(ListInvoicesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        InvoiceFilter filter = new InvoiceFilter(
                query.status(),
                query.vendorId(),
                query.poId(),
                query.overdueOnly(),
                LocalDate.now(clock),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size());

        log.info("[ACTION] Start ListInvoices | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.page(),
                query.size());
        var items = invoiceRepository.findByFilter(filter).stream()
                .map(InvoiceView::from)
                .toList();
        long total = invoiceRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListInvoices | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, query.page(), query.size(), DEFAULT_SORT));
    }
}
