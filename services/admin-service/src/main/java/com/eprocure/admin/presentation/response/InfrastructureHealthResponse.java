package com.eprocure.admin.presentation.response;

public record InfrastructureHealthResponse(
        InfrastructureComponentResponse postgresql,
        InfrastructureComponentResponse redis,
        InfrastructureComponentResponse kafka) {
}
