package com.eprocure.vendor.presentation.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record VendorContactResponse(
        UUID id,
        String name,
        String role,
        String email,
        String phone,
        @JsonProperty("isPrimary")
        boolean primary) {
}
