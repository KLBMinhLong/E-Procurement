package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.ListVendorsQuery;
import com.eprocure.vendor.application.service.PageMeta;
import com.eprocure.vendor.application.service.PageResult;
import com.eprocure.vendor.application.service.VendorSummaryView;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.util.Locale;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListVendorsUseCase {
    private static final Logger log = LogManager.getLogger(ListVendorsUseCase.class);

    private final VendorRepository vendorRepository;

    public ListVendorsUseCase(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<VendorSummaryView> execute(ListVendorsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        int page = Math.max(query.page(), 1);
        int size = Math.min(Math.max(query.size(), 1), 100);
        VendorSort sort = VendorSort.from(query.sort());

        log.info("[ACTION] Start ListVendors | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                page,
                size);

        VendorFilter filter = new VendorFilter(
                query.status(),
                normalizeCategory(query.category()),
                query.onAvlOnly(),
                normalize(query.query()),
                page,
                size,
                (page - 1) * size,
                sort.field(),
                sort.direction());

        var items = vendorRepository.findByFilter(filter).stream()
                .map(VendorSummaryView::from)
                .toList();
        long total = vendorRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListVendors | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, page, size, sort.normalized()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeCategory(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private record VendorSort(String field, String direction) {
        static VendorSort from(String rawSort) {
            String field = "createdAt";
            String direction = "desc";
            if (rawSort != null && !rawSort.isBlank()) {
                String[] parts = rawSort.split(",", 2);
                field = allowedField(parts[0].trim());
                if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                    direction = "asc";
                }
            }
            return new VendorSort(field, direction);
        }

        String normalized() {
            return field + "," + direction;
        }

        private static String allowedField(String value) {
            return switch (value) {
                case "vendorCode", "name", "status", "overallScore", "createdAt" -> value;
                default -> "createdAt";
            };
        }
    }
}
