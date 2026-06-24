package com.eprocure.admin.infrastructure.configuration;

import com.eprocure.admin.domain.model.EnvVariable;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import com.eprocure.admin.infrastructure.config.AdminProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigBackedServiceConfigRepository implements ServiceConfigRepository {
    private static final Logger log = LogManager.getLogger(ConfigBackedServiceConfigRepository.class);

    private final AdminProperties properties;

    public ConfigBackedServiceConfigRepository(AdminProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<ServiceConfig> findAll() {
        log.debug("[REPO] findAll service_configs");
        return properties.getServices().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ServiceConfig> findByName(String serviceName) {
        String normalized = normalize(serviceName);
        log.debug("[REPO] findByName service_config | service={}", normalized);
        return properties.getServices().stream()
                .filter(service -> normalize(service.getName()).equals(normalized))
                .findFirst()
                .map(this::toDomain);
    }

    private ServiceConfig toDomain(AdminProperties.ServiceEntry entry) {
        return new ServiceConfig(
                entry.getName(),
                entry.getDisplayName().isBlank() ? entry.getName() : entry.getDisplayName(),
                entry.getBaseUrl(),
                ServiceStatus.UNKNOWN,
                optional(entry.getVersion()),
                entry.getVariables().stream().map(this::toVariable).toList(),
                Optional.empty());
    }

    private EnvVariable toVariable(AdminProperties.VariableEntry entry) {
        return new EnvVariable(
                entry.getKey(),
                entry.getValue(),
                entry.isSensitive(),
                optional(entry.getDescription()),
                Optional.<Instant>empty(),
                Optional.of("system"));
    }

    private Optional<String> optional(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(item -> !item.isBlank());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
