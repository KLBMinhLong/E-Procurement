package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.MatchStatus;

public record InvoiceMatchResponse(
        MatchStatus matchStatus,
        MatchResultResponse matchResult,
        boolean requiresManualReview) {

    public record MatchResultResponse(
            MatchStatus poMatchStatus,
            MatchStatus grMatchStatus,
            String qtyVariance,
            String priceVariance) {
    }
}
