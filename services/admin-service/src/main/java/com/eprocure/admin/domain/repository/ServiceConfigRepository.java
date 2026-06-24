package com.eprocure.admin.domain.repository;

import com.eprocure.admin.domain.model.ServiceConfig;
import java.util.List;
import java.util.Optional;

public interface ServiceConfigRepository {
    List<ServiceConfig> findAll();

    Optional<ServiceConfig> findByName(String serviceName);
}
