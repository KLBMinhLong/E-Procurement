package com.eprocure.vendor.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateVendorRequest(
        @NotBlank @Size(max = 300) String name,
        @NotBlank String taxCode,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @Valid VendorAddressRequest address,
        @NotEmpty List<@NotBlank String> categories,
        @Valid List<VendorContactRequest> contacts,
        String notes) {
}
