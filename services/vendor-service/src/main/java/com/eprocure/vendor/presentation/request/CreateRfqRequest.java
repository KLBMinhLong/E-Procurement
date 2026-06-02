package com.eprocure.vendor.presentation.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateRfqRequest(
        @NotNull UUID prId,
        @NotBlank String title,
        @NotNull @Future Instant submissionDeadline,
        @NotEmpty @Size(min = 2) List<@NotNull UUID> invitedVendorIds,
        String requirements,
        List<UUID> attachmentIds) {
}
