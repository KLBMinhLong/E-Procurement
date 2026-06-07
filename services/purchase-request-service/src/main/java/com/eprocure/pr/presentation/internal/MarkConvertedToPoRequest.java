package com.eprocure.pr.presentation.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MarkConvertedToPoRequest(
        @NotNull UUID poId,
        @NotBlank String poNumber) {
}
