package com.eprocure.approval.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RequestChangesRequest(
        @NotBlank @Size(min = 20, max = 2000) String comment,
        List<String> requestedFields) {
}
