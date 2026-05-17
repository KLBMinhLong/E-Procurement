package com.eprocure.iam.infrastructure.keycloak;

import com.eprocure.iam.application.port.out.CredentialVerificationPort;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KeycloakCredentialVerificationAdapter implements CredentialVerificationPort {
    private static final Logger log = LogManager.getLogger(KeycloakCredentialVerificationAdapter.class);
    private final RestClient keycloakRestClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakCredentialVerificationAdapter(
            RestClient keycloakRestClient,
            @Value("${eprocure.keycloak.realm:eprocure}") String realm,
            @Value("${eprocure.keycloak.client-id:eprocure-iam}") String clientId,
            @Value("${eprocure.keycloak.client-secret:dev-secret}") String clientSecret) {
        this.keycloakRestClient = keycloakRestClient;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean verify(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);

        try {
            keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            log.warn("[SECURITY] Keycloak credential rejected | status={}", exception.getStatusCode().value());
            return false;
        }
    }
}
