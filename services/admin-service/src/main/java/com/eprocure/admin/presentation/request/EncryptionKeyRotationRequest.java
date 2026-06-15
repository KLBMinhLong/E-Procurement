package com.eprocure.admin.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record EncryptionKeyRotationRequest(
        @NotBlank String confirmationCode,
        Integer keySize) {
}
