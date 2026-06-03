package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceMatchResult(
        MatchStatus matchStatus,
        MatchStatus poMatchStatus,
        MatchStatus grMatchStatus,
        BigDecimal qtyVariance,
        Money priceVariance,
        Instant matchedAt,
        boolean requiresManualReview,
        boolean replayed) {
}
