package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.eprocure.admin.application.port.out.InfrastructureHealthProbePort;
import com.eprocure.admin.application.port.out.ServiceHealthProbePort;
import com.eprocure.admin.domain.model.InfrastructureComponentHealth;
import com.eprocure.admin.domain.model.InfrastructureHealth;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceHealth;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetSystemHealthUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ServiceConfigRepository repository;

    @Mock
    private ServiceHealthProbePort serviceHealthProbePort;

    @Mock
    private InfrastructureHealthProbePort infrastructureHealthProbePort;

    @Test
    @DisplayName("System health là UP khi mọi service và infrastructure đều UP")
    void should_return_up_when_all_components_are_up() {
        // Given
        ServiceConfig iam = serviceConfig("iam-service");
        given(repository.findAll()).willReturn(List.of(iam));
        given(serviceHealthProbePort.check(iam))
                .willReturn(new ServiceHealth("iam-service", ServiceStatus.UP, 9, Optional.empty(), NOW));
        given(infrastructureHealthProbePort.check()).willReturn(infrastructure(ServiceStatus.UP, ServiceStatus.UP, ServiceStatus.UP));
        GetSystemHealthUseCase useCase = new GetSystemHealthUseCase(
                repository,
                serviceHealthProbePort,
                infrastructureHealthProbePort,
                CLOCK);

        // When
        var result = useCase.execute(ACTOR_ID);

        // Then
        assertThat(result.overallStatus()).isEqualTo(ServiceStatus.UP);
        assertThat(result.checkedAt()).isEqualTo(NOW);
        assertThat(result.services()).extracting(ServiceHealth::name).containsExactly("iam-service");
    }

    @Test
    @DisplayName("System health là DOWN khi một component bị DOWN")
    void should_return_down_when_any_component_is_down() {
        // Given
        ServiceConfig iam = serviceConfig("iam-service");
        given(repository.findAll()).willReturn(List.of(iam));
        given(serviceHealthProbePort.check(iam))
                .willReturn(new ServiceHealth("iam-service", ServiceStatus.UP, 9, Optional.empty(), NOW));
        given(infrastructureHealthProbePort.check()).willReturn(infrastructure(ServiceStatus.UP, ServiceStatus.DOWN, ServiceStatus.UP));
        GetSystemHealthUseCase useCase = new GetSystemHealthUseCase(
                repository,
                serviceHealthProbePort,
                infrastructureHealthProbePort,
                CLOCK);

        // When
        var result = useCase.execute(ACTOR_ID);

        // Then
        assertThat(result.overallStatus()).isEqualTo(ServiceStatus.DOWN);
    }

    private ServiceConfig serviceConfig(String serviceName) {
        return new ServiceConfig(
                serviceName,
                serviceName,
                "http://localhost:8081",
                ServiceStatus.UNKNOWN,
                Optional.empty(),
                List.of(),
                Optional.empty());
    }

    private InfrastructureHealth infrastructure(ServiceStatus postgresql, ServiceStatus redis, ServiceStatus kafka) {
        return new InfrastructureHealth(
                new InfrastructureComponentHealth("postgresql", postgresql, Map.of()),
                new InfrastructureComponentHealth("redis", redis, Map.of()),
                new InfrastructureComponentHealth("kafka", kafka, Map.of()));
    }
}
