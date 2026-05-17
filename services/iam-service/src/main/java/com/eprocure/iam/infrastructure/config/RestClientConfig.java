package com.eprocure.iam.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient keycloakRestClient(
            RestClient.Builder builder,
            @Value("${eprocure.keycloak.url:http://localhost:18080}") String keycloakUrl) {
        return builder.baseUrl(keycloakUrl).build();
    }
}
