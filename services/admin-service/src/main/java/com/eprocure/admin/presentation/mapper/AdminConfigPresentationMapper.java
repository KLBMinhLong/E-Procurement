package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.ConfigVariableChange;
import com.eprocure.admin.application.port.in.RestartServiceCommand;
import com.eprocure.admin.application.port.in.RotateEncryptionKeyCommand;
import com.eprocure.admin.application.port.in.UpdateServiceConfigCommand;
import com.eprocure.admin.application.service.EncryptionKeyRotationResult;
import com.eprocure.admin.application.service.ServiceConfigUpdateResult;
import com.eprocure.admin.application.service.ServiceRestartResult;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.domain.model.EnvVariable;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.presentation.request.EncryptionKeyRotationRequest;
import com.eprocure.admin.presentation.request.ServiceConfigUpdateRequest;
import com.eprocure.admin.presentation.request.ServiceRestartRequest;
import com.eprocure.admin.presentation.response.EncryptionKeyRotationResponse;
import com.eprocure.admin.presentation.response.EnvVariableResponse;
import com.eprocure.admin.presentation.response.ServiceConfigResponse;
import com.eprocure.admin.presentation.response.ServiceConfigSummaryResponse;
import com.eprocure.admin.presentation.response.ServiceConfigUpdateResponse;
import com.eprocure.admin.presentation.response.ServiceRestartResponse;
import java.util.List;
import java.util.Optional;
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

    public UpdateServiceConfigCommand toCommand(
            String serviceName,
            ServiceConfigUpdateRequest request,
            UserPrincipal principal) {
        return new UpdateServiceConfigCommand(
                principal.getId(),
                serviceName,
                request.variables().stream()
                        .map(variable -> new ConfigVariableChange(
                                variable.key(),
                                variable.value(),
                                variable.isSensitive(),
                                Optional.ofNullable(variable.description())))
                        .toList(),
                request.confirmationCode(),
                request.requiresRestart(),
                request.changeReason());
    }

    public RestartServiceCommand toCommand(
            String serviceName,
            ServiceRestartRequest request,
            UserPrincipal principal) {
        return new RestartServiceCommand(
                principal.getId(),
                serviceName,
                request.confirmationCode(),
                request.reason());
    }

    public RotateEncryptionKeyCommand toCommand(
            EncryptionKeyRotationRequest request,
            UserPrincipal principal) {
        return new RotateEncryptionKeyCommand(
                principal.getId(),
                request.confirmationCode(),
                request.keySize() == null ? 2048 : request.keySize());
    }

    public ServiceConfigUpdateResponse toResponse(ServiceConfigUpdateResult result) {
        return new ServiceConfigUpdateResponse(
                result.actionId(),
                result.status().name(),
                result.updatedCount(),
                result.requiresRestart(),
                result.affectedService(),
                result.applied());
    }

    public ServiceRestartResponse toResponse(ServiceRestartResult result) {
        return new ServiceRestartResponse(
                result.actionId(),
                result.status().name(),
                result.estimatedDowntimeSeconds(),
                result.triggeredAt(),
                result.applied());
    }

    public EncryptionKeyRotationResponse toResponse(EncryptionKeyRotationResult result) {
        return new EncryptionKeyRotationResponse(
                result.actionId(),
                result.status().name(),
                result.newKeyVersion(),
                result.rotatedAt(),
                result.oldKeyRetiredAt(),
                result.applied());
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
