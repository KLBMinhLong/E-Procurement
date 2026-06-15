package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.domain.model.InfrastructureComponentHealth;
import com.eprocure.admin.domain.model.InfrastructureHealth;
import com.eprocure.admin.domain.model.ServiceHealth;
import com.eprocure.admin.domain.model.SystemHealth;
import com.eprocure.admin.presentation.response.InfrastructureComponentResponse;
import com.eprocure.admin.presentation.response.InfrastructureHealthResponse;
import com.eprocure.admin.presentation.response.ServiceHealthResponse;
import com.eprocure.admin.presentation.response.SystemHealthResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminHealthPresentationMapper {

    public SystemHealthResponse toResponse(SystemHealth health) {
        return new SystemHealthResponse(
                health.overallStatus().name(),
                health.services().stream().map(this::toResponse).toList(),
                toResponse(health.infrastructure()),
                health.checkedAt());
    }

    private ServiceHealthResponse toResponse(ServiceHealth health) {
        return new ServiceHealthResponse(
                health.name(),
                health.status().name(),
                health.responseTime(),
                health.uptime().orElse(null),
                health.lastCheck());
    }

    private InfrastructureHealthResponse toResponse(InfrastructureHealth health) {
        return new InfrastructureHealthResponse(
                toResponse(health.postgresql()),
                toResponse(health.redis()),
                toResponse(health.kafka()));
    }

    private InfrastructureComponentResponse toResponse(InfrastructureComponentHealth health) {
        return new InfrastructureComponentResponse(
                health.status().name(),
                integerMetric(health, "connections"),
                integerMetric(health, "maxConnections"),
                stringMetric(health, "usedMemory"),
                stringMetric(health, "maxMemory"),
                integerMetric(health, "keyCount"),
                integerMetric(health, "topicCount"));
    }

    private Integer integerMetric(InfrastructureComponentHealth health, String key) {
        Object value = health.metrics().get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private String stringMetric(InfrastructureComponentHealth health, String key) {
        Object value = health.metrics().get(key);
        return value == null ? null : value.toString();
    }
}
