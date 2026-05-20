package com.eprocure.pr.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for PUT /api/v1/purchase-requests/{id}.
 * All fields are optional — only non-null fields are applied (but lineItems, if present, replaces entirely).
 */
public record UpdatePrRequest(
        @Size(max = 500)
        String title,

        @Size(min = 50)
        String justification,

        String priority,
        String urgencyReason,
        LocalDate needByDate,

        @Valid
        @NotEmpty
        List<PrLineItemRequest> lineItems
) {}
