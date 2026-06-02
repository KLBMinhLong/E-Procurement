package com.eprocure.vendor.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AwardRfqRequest(
        @NotNull UUID awardedQuoteId,
        @Size(min = 20) String awardReason) {
}
