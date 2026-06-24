package com.eprocure.admin.infrastructure.health;

import com.eprocure.admin.application.port.out.ServiceHealthProbePort;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceHealth;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.infrastructure.config.AdminProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class HttpServiceHealthProbeAdapter implements ServiceHealthProbePort {
    private static final Logger log = LogManager.getLogger(HttpServiceHealthProbeAdapter.class);
    private static final String HEALTH_PATH = "/actuator/health";

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration timeout;

    public HttpServiceHealthProbeAdapter(ObjectMapper objectMapper, Clock clock, AdminProperties properties) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.timeout = properties.getHealthTimeout();
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public ServiceHealth check(ServiceConfig serviceConfig) {
        Instant checkedAt = Instant.now(clock);
        URI uri = URI.create(stripTrailingSlash(serviceConfig.baseUrl()) + HEALTH_PATH);
        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = elapsedMs(started);
            ServiceStatus status = resolveStatus(response.statusCode(), extractStatus(response.body()));
            return new ServiceHealth(serviceConfig.serviceName(), status, elapsedMs, Optional.empty(), checkedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.debug("[ACTION] Service health probe interrupted | service={}", serviceConfig.serviceName());
            return new ServiceHealth(serviceConfig.serviceName(), ServiceStatus.UNKNOWN, elapsedMs(started), Optional.empty(), checkedAt);
        } catch (Exception exception) {
            log.debug("[ACTION] Service health probe failed | service={} | reason={}",
                    serviceConfig.serviceName(),
                    exception.getClass().getSimpleName());
            return new ServiceHealth(serviceConfig.serviceName(), ServiceStatus.DOWN, elapsedMs(started), Optional.empty(), checkedAt);
        }
    }

    private ServiceStatus resolveStatus(int httpStatus, Optional<String> status) {
        if (httpStatus >= 500) {
            return ServiceStatus.DOWN;
        }
        if (httpStatus >= 400) {
            return ServiceStatus.DEGRADED;
        }
        return status
                .map(value -> switch (value.toUpperCase(Locale.ROOT)) {
                    case "UP" -> ServiceStatus.UP;
                    case "DOWN", "OUT_OF_SERVICE" -> ServiceStatus.DOWN;
                    default -> ServiceStatus.DEGRADED;
                })
                .orElse(ServiceStatus.UNKNOWN);
    }

    private Optional<String> extractStatus(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode status = root.get("status");
            return status == null || status.asText().isBlank() ? Optional.empty() : Optional.of(status.asText());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
