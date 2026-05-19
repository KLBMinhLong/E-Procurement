package com.eprocure.pr.presentation.request;

import com.eprocure.pr.domain.model.PrPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePrRequest(
        @NotBlank
        @Size(max = 500)
        String title,

        @NotBlank
        @Size(min = 50)
        String justification,

        PrPriority priority,
        String urgencyReason,
        LocalDate needByDate,

        @Valid
        @NotEmpty
        List<PrLineItemRequest> lineItems,

        List<UUID> attachmentIds) {
}
