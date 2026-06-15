package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.domain.model.EnvVariable;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.presentation.response.EnvVariableResponse;
import com.eprocure.admin.presentation.response.ServiceConfigResponse;
import com.eprocure.admin.presentation.response.ServiceConfigSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminConfigPresentationMapper {

    public List<ServiceConfigResponse> toResponseList(List<ServiceConfig> services) {
        return services.stream().map(this::toResponse).toList();
    }

    public ServiceConfigResponse toResponse(ServiceConfig config) {
        return new ServiceConfigResponse(
                config.serviceName(),
                config.displayName(),
                config.status().name(),
                config.version().orElse(null),
                config.variables().stream().map(this::toResponse).toList(),
                config.lastHealthCheck().orElse(null));
    }

    public ServiceConfigSummaryResponse toSummary(List<ServiceConfig> services) {
        return new ServiceConfigSummaryResponse(
                services.size(),
                countByStatus(services, ServiceStatus.UP),
                countByStatus(services, ServiceStatus.DOWN),
                countByStatus(services, ServiceStatus.DEGRADED),
                countByStatus(services, ServiceStatus.UNKNOWN));
    }

    private EnvVariableResponse toResponse(EnvVariable variable) {
        return new EnvVariableResponse(
                variable.key(),
                variable.value(),
                variable.sensitive(),
                variable.description().orElse(null),
                variable.lastUpdatedAt().orElse(null),
                variable.lastUpdatedBy().orElse(null));
    }

    private int countByStatus(List<ServiceConfig> services, ServiceStatus status) {
        return (int) services.stream().filter(service -> service.status() == status).count();
    }
}
