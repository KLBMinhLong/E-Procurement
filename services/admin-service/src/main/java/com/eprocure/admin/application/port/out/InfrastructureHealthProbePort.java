package com.eprocure.admin.application.port.out;

import com.eprocure.admin.domain.model.InfrastructureHealth;

public interface InfrastructureHealthProbePort {
    InfrastructureHealth check();
}
