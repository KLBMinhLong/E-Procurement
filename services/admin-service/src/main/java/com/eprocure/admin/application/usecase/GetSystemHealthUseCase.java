package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.out.InfrastructureHealthProbePort;
import com.eprocure.admin.application.port.out.ServiceHealthProbePort;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.InfrastructureComponentHealth;
import com.eprocure.admin.domain.model.InfrastructureHealth;
import com.eprocure.admin.domain.model.ServiceHealth;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.domain.model.SystemHealth;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class GetSystemHealthUseCase {
    private static final Logger log = LogManager.getLogger(GetSystemHealthUseCase.class);

    private final ServiceConfigRepository repository;
    private final ServiceHealthProbePort serviceHealthProbePort;
    private final InfrastructureHealthProbePort infrastructureHealthProbePort;
    private final Clock clock;

    public GetSystemHealthUseCase(
            ServiceConfigRepository repository,
            ServiceHealthProbePort serviceHealthProbePort,
            InfrastructureHealthProbePort infrastructureHealthProbePort,
            Clock clock) {
        this.repository = repository;
        this.serviceHealthProbePort = serviceHealthProbePort;
        this.infrastructureHealthProbePort = infrastructureHealthProbePort;
        this.clock = clock;
    }

    public SystemHealth execute(UUID actorId) {
        log.info("[ACTION] Start GetSystemHealth | userId={}", LogMaskingUtil.maskId(actorId));
        List<ServiceHealth> services = repository.findAll().stream()
                .map(serviceHealthProbePort::check)
                .toList();
        InfrastructureHealth infrastructure = infrastructureHealthProbePort.check();
        ServiceStatus overall = resolveOverallStatus(services, infrastructure);
        SystemHealth result = new SystemHealth(overall, services, infrastructure, Instant.now(clock));
        log.info("[ACTION] Complete GetSystemHealth | userId={} | overall={} | services={}",
                LogMaskingUtil.maskId(actorId),
                result.overallStatus(),
                services.size());
        return result;
    }

    private ServiceStatus resolveOverallStatus(List<ServiceHealth> services, InfrastructureHealth infrastructure) {
        List<ServiceStatus> statuses = Stream.concat(
                        services.stream().map(ServiceHealth::status),
                        Stream.of(infrastructure.postgresql(), infrastructure.redis(), infrastructure.kafka())
                                .map(InfrastructureComponentHealth::status))
                .toList();
        if (statuses.stream().anyMatch(ServiceStatus.DOWN::equals)) {
            return ServiceStatus.DOWN;
        }
        if (statuses.stream().anyMatch(status -> status == ServiceStatus.DEGRADED || status == ServiceStatus.UNKNOWN)) {
            return ServiceStatus.DEGRADED;
        }
        return ServiceStatus.UP;
    }
}
