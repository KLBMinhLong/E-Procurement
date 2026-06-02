package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.ListRfqsQuery;
import com.eprocure.vendor.application.service.PageMeta;
import com.eprocure.vendor.application.service.PageResult;
import com.eprocure.vendor.application.service.RfqDetailView;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.repository.RfqFilter;
import com.eprocure.vendor.domain.repository.RfqRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListRfqsUseCase {
    private static final Logger log = LogManager.getLogger(ListRfqsUseCase.class);

    private final RfqRepository rfqRepository;

    public ListRfqsUseCase(RfqRepository rfqRepository) {
        this.rfqRepository = rfqRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<RfqDetailView> execute(ListRfqsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        int page = Math.max(query.page(), 1);
        int size = Math.min(Math.max(query.size(), 1), 100);
        RfqSort sort = RfqSort.from(query.sort());

        log.info("[ACTION] Start ListRfqs | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                page,
                size);

        RfqFilter filter = new RfqFilter(
                query.status(),
                query.prId(),
                page,
                size,
                (page - 1) * size,
                sort.field(),
                sort.direction());
        var items = rfqRepository.findByFilter(filter).stream()
                .map(RfqDetailView::from)
                .toList();
        long total = rfqRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListRfqs | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, page, size, sort.normalized()));
    }

    private record RfqSort(String field, String direction) {
        static RfqSort from(String rawSort) {
            String field = "createdAt";
            String direction = "desc";
            if (rawSort != null && !rawSort.isBlank()) {
                String[] parts = rawSort.split(",", 2);
                field = allowedField(parts[0].trim());
                if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                    direction = "asc";
                }
            }
            return new RfqSort(field, direction);
        }

        String normalized() {
            return field + "," + direction;
        }

        private static String allowedField(String value) {
            return switch (value) {
                case "rfqNumber", "status", "submissionDeadline", "createdAt" -> value;
                default -> "createdAt";
            };
        }
    }
}
