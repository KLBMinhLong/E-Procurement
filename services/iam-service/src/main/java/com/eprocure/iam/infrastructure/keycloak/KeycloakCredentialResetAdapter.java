package com.eprocure.iam.infrastructure.keycloak;

import com.eprocure.iam.application.port.out.CredentialResetPort;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KeycloakCredentialResetAdapter implements CredentialResetPort {
    private static final Logger log = LogManager.getLogger(KeycloakCredentialResetAdapter.class);
    private final RestClient keycloakRestClient;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakCredentialResetAdapter(
            RestClient keycloakRestClient,
            @Value("${eprocure.keycloak.realm:eprocure}") String realm,
            @Value("${eprocure.keycloak.admin-user:admin}") String adminUsername,
            @Value("${eprocure.keycloak.admin-password:admin}") String adminPassword) {
        this.keycloakRestClient = keycloakRestClient;
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void resetPassword(String keycloakUsername, String newPassword) {
        try {
            String accessToken = adminAccessToken();
            KeycloakUser keycloakUser = findUser(accessToken, keycloakUsername);
            keycloakRestClient.put()
                    .uri("/admin/realms/{realm}/users/{userId}/reset-password", realm, keycloakUser.id())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PasswordCredential("password", newPassword, false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.error("[EXCEPTION][SYS_002] Keycloak password reset failed | username={}", keycloakUsername, exception);
            throw new BusinessException(ErrorCode.SYS_002);
        }
    }

    private String adminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        AdminTokenResponse tokenResponse = keycloakRestClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AdminTokenResponse.class);
        if (tokenResponse == null || tokenResponse.access_token() == null || tokenResponse.access_token().isBlank()) {
            throw new BusinessException(ErrorCode.SYS_002);
        }
        return tokenResponse.access_token();
    }

    private KeycloakUser findUser(String accessToken, String keycloakUsername) {
        List<KeycloakUser> users = keycloakRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", keycloakUsername)
                        .queryParam("exact", true)
                        .build(realm))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return users == null || users.isEmpty()
                ? missingKeycloakUser(keycloakUsername)
                : users.get(0);
    }

    private KeycloakUser missingKeycloakUser(String keycloakUsername) {
        log.error("[EXCEPTION][SYS_002] Keycloak user not found | username={}", keycloakUsername);
        throw new BusinessException(ErrorCode.SYS_002);
    }

    private record AdminTokenResponse(String access_token) {
    }

    private record KeycloakUser(String id, String username) {
    }

    private record PasswordCredential(String type, String value, boolean temporary) {
    }
}
