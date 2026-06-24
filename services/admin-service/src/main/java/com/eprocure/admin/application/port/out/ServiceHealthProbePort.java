package com.eprocure.admin.application.port.out;

import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceHealth;

public interface ServiceHealthProbePort {
    ServiceHealth check(ServiceConfig serviceConfig);
}
