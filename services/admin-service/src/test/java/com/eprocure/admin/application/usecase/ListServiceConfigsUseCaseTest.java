package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.eprocure.admin.application.port.out.ServiceHealthProbePort;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.domain.model.EnvVariable;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceHealth;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListServiceConfigsUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final Instant CHECKED_AT = Instant.parse("2026-06-15T01:00:00Z");

    @Mock
    private ServiceConfigRepository repository;

    @Mock
    private ServiceHealthProbePort healthProbePort;

    @InjectMocks
    private ListServiceConfigsUseCase listUseCase;

    @Test
    @DisplayName("Danh sách service config phải mask biến nhạy cảm")
    void should_mask_sensitive_values_when_listing_service_configs() {
        // Given
        ServiceConfig config = serviceConfig("iam-service");
        given(repository.findAll()).willReturn(List.of(config));
        given(healthProbePort.check(config))
                .willReturn(new ServiceHealth("iam-service", ServiceStatus.UP, 12, Optional.empty(), CHECKED_AT));

        // When
        List<ServiceConfig> result = listUseCase.execute(ACTOR_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ServiceStatus.UP);
        assertThat(result.get(0).lastHealthCheck()).contains(CHECKED_AT);
        assertThat(result.get(0).variables())
                .extracting(EnvVariable::value)
                .containsExactly("db_iam", "***");
    }

    @Test
    @DisplayName("Ném lỗi khi service config không tồn tại")
    void should_throw_when_service_config_not_found() {
        // Given
        GetServiceConfigUseCase getUseCase = new GetServiceConfigUseCase(repository, healthProbePort);
        given(repository.findByName("missing-service")).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> getUseCase.execute("missing-service", ACTOR_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_CONFIG_NOT_FOUND.code());
                });
    }

    private ServiceConfig serviceConfig(String serviceName) {
        return new ServiceConfig(
                serviceName,
                "IAM Service",
                "http://localhost:8081",
                ServiceStatus.UNKNOWN,
                Optional.of("0.1.0-SNAPSHOT"),
                List.of(
                        new EnvVariable(
                                "IAM_DB_NAME",
                                "db_iam",
                                false,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of("system")),
                        new EnvVariable(
                                "IAM_DB_PASS",
                                "iam_pass_dev",
                                true,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of("system"))),
                Optional.empty());
    }
}
