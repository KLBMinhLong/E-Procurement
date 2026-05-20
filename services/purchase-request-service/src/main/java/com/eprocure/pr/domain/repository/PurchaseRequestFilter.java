package com.eprocure.pr.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pure POJO filter for querying purchase requests.
 * No Spring or framework imports.
 */
public record PurchaseRequestFilter(
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
