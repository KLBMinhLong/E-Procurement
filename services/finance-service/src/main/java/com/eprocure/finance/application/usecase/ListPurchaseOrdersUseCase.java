package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ListPurchaseOrdersQuery;
import com.eprocure.finance.application.service.PageMeta;
import com.eprocure.finance.application.service.PageResult;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.application.service.PurchaseOrderViewAssembler;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.repository.PurchaseOrderFilter;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPurchaseOrdersUseCase {
    private static final Logger log = LogManager.getLogger(ListPurchaseOrdersUseCase.class);
    private static final String PERMISSION_VIEW_ALL = "PO_VIEW_ALL";
    private static final String PERMISSION_VIEW_OWN = "PO_VIEW_OWN";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderViewAssembler viewAssembler;

    public ListPurchaseOrdersUseCase(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderViewAssembler viewAssembler) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.viewAssembler = viewAssembler;
    }

    @Transactional(readOnly = true)
    public PageResult<PurchaseOrderView> execute(ListPurchaseOrdersQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        if (!query.hasPermission(PERMISSION_VIEW_ALL) && !query.hasPermission(PERMISSION_VIEW_OWN)) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
        PurchaseOrderSort sort = PurchaseOrderSort.from(query.sort());
        UUID purchasingOfficerId = query.hasPermission(PERMISSION_VIEW_ALL) ? null : query.actorId();
        PurchaseOrderFilter filter = new PurchaseOrderFilter(
                purchasingOfficerId,
                query.status(),
                query.vendorId(),
                query.prId(),
                toStartInstant(query.fromDate()),
                toEndExclusiveInstant(query.toDate()),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size(),
                sort.field(),
                sort.direction());

        log.info("[ACTION] Start ListPurchaseOrders | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.page(),
                query.size());
        var items = viewAssembler.toViews(purchaseOrderRepository.findByFilter(filter));
        long total = purchaseOrderRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListPurchaseOrders | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, query.page(), query.size(), sort.normalized()));
    }

    private java.time.Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private java.time.Instant toEndExclusiveInstant(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private record PurchaseOrderSort(String field, String direction) {
        static PurchaseOrderSort from(String rawSort) {
            String field = "createdAt";
            String direction = "desc";
            if (rawSort != null && !rawSort.isBlank()) {
                String[] parts = rawSort.split(",", 2);
                field = allowedField(parts[0].trim());
                if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                    direction = "asc";
                }
            }
            return new PurchaseOrderSort(field, direction);
        }

        String normalized() {
            return field + "," + direction;
        }

        private static String allowedField(String value) {
            return switch (value) {
                case "poNumber", "status", "vendorName", "totalAmount", "createdAt" -> value;
                default -> "createdAt";
            };
        }
    }
}
