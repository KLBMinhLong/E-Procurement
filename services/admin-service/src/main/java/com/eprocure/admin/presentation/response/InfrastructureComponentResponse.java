package com.eprocure.admin.presentation.response;

public record InfrastructureComponentResponse(
        String status,
        Integer connections,
        Integer maxConnections,
        String usedMemory,
        String maxMemory,
        Integer keyCount,
        Integer topicCount) {
}
