package com.eprocure.pr.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH /api/v1/purchase-requests/{id}/cancel.
 */
public record CancelPrRequest(
        @NotBlank
        @Size(min = 10)
        String reason
) {}
