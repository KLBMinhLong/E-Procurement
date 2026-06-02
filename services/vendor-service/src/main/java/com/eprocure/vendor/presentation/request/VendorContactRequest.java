package com.eprocure.vendor.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VendorContactRequest(
        @NotBlank String name,
        String role,
        @NotBlank @Email String email,
        @NotBlank String phone,
        boolean isPrimary) {
}
