package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.out.ServiceHealthProbePort;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class GetServiceConfigUseCase {
    private static final Logger log = LogManager.getLogger(GetServiceConfigUseCase.class);

    private final ServiceConfigRepository repository;
    private final ServiceHealthProbePort healthProbePort;

    public GetServiceConfigUseCase(
            ServiceConfigRepository repository,
            ServiceHealthProbePort healthProbePort) {
        this.repository = repository;
        this.healthProbePort = healthProbePort;
    }

    public ServiceConfig execute(String serviceName, UUID actorId) {
        log.info("[ACTION] Start GetServiceConfig | service={} | userId={}",
                serviceName,
                LogMaskingUtil.maskId(actorId));
        ServiceConfig config = repository.findByName(serviceName)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_CONFIG_NOT_FOUND));
        var health = healthProbePort.check(config);
        ServiceConfig result = config.withHealth(health.status(), health.lastCheck()).masked();
        log.info("[ACTION] Complete GetServiceConfig | service={} | userId={} | status={}",
                serviceName,
                LogMaskingUtil.maskId(actorId),
                result.status());
        return result;
    }
}
