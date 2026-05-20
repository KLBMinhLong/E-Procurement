package com.eprocure.pr.presentation.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Query parameters for GET /api/v1/purchase-requests.
 */
public record ListPrRequest(
        String status,
        String priority,
        UUID departmentId,
        UUID requesterId,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String q,
        Integer page,
        Integer size,
        String sort
) {}
