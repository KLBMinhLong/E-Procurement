package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight summary view for list endpoint (GET /purchase-requests).
 */
public record PurchaseRequestSummaryView(
        UUID id,
        String prNumber,
        String title,
        PrPriority priority,
        PrStatus status,
        Money totalAmount,
        UUID requesterId,
        UUID departmentId,
        LocalDate needByDate,
        Instant createdAt,
        Instant updatedAt
) {}
