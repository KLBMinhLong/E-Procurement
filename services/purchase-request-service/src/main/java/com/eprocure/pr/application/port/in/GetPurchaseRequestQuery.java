package com.eprocure.pr.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Query parameters for listing/filtering purchase requests.
 */
public record GetPurchaseRequestQuery(
        UUID actorId,
        String viewScope,       // OWN | DEPARTMENT | ALL
        String status,
        String priority,
        UUID departmentId,
        UUID requesterId,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String q,
        int page,
        int size,
        String sort
) {}
