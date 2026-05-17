package com.eprocure.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

final class EprocureIamClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String iamBaseUrl;
    private final String internalApiKey;

    EprocureIamClient(String iamBaseUrl, String internalApiKey, Duration timeout) {
        this.iamBaseUrl = normalizeBaseUrl(iamBaseUrl);
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    Optional<EprocureUserRepresentation> findById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return sendUserLookup("/internal/keycloak/users/" + encode(userId));
    }

    Optional<EprocureUserRepresentation> findByLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        return sendUserLookup("/internal/keycloak/users?login=" + encode(login));
    }

    boolean verifyPassword(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", password));
            HttpRequest request = requestBuilder("/internal/keycloak/credentials/verify")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }
            JsonNode validNode = objectMapper.readTree(response.body()).path("data").path("valid");
            return validNode.asBoolean(false);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private Optional<EprocureUserRepresentation> sendUserLookup(String pathAndQuery) {
        try {
            HttpRequest request = requestBuilder(pathAndQuery).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode data = objectMapper.readTree(response.body()).path("data");
            if (data.isMissingNode() || data.isNull()) {
                return Optional.empty();
            }
            return Optional.of(new EprocureUserRepresentation(
                    data.path("id").asText(),
                    data.path("username").asText(),
                    data.path("email").asText(),
                    data.path("fullName").asText(),
                    data.path("enabled").asBoolean(false)));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private HttpRequest.Builder requestBuilder(String pathAndQuery) {
        return HttpRequest.newBuilder(URI.create(iamBaseUrl + pathAndQuery))
                .timeout(Duration.ofSeconds(5))
                .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                .header("Accept", "application/json");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://iam-service:8081" : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
