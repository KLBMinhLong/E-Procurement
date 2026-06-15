package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.out.ServiceHealthProbePort;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ListServiceConfigsUseCase {
    private static final Logger log = LogManager.getLogger(ListServiceConfigsUseCase.class);

    private final ServiceConfigRepository repository;
    private final ServiceHealthProbePort healthProbePort;

    public ListServiceConfigsUseCase(
            ServiceConfigRepository repository,
            ServiceHealthProbePort healthProbePort) {
        this.repository = repository;
        this.healthProbePort = healthProbePort;
    }

    public List<ServiceConfig> execute(UUID actorId) {
        log.info("[ACTION] Start ListServiceConfigs | userId={}", LogMaskingUtil.maskId(actorId));
        List<ServiceConfig> services = repository.findAll().stream()
                .map(this::withCurrentHealth)
                .map(ServiceConfig::masked)
                .toList();
        log.info("[ACTION] Complete ListServiceConfigs | userId={} | total={}",
                LogMaskingUtil.maskId(actorId),
                services.size());
        return services;
    }

    private ServiceConfig withCurrentHealth(ServiceConfig config) {
        var health = healthProbePort.check(config);
        return config.withHealth(health.status(), health.lastCheck());
    }
}
