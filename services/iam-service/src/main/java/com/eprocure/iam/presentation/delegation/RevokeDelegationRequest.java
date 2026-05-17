package com.eprocure.iam.presentation.delegation;

import jakarta.validation.constraints.Size;

public record RevokeDelegationRequest(@Size(max = 500) String reason) {
}
