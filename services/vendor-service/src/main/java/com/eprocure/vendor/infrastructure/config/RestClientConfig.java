package com.eprocure.vendor.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient purchaseRequestRestClient(
            RestClient.Builder builder,
            @Value("${eprocure.vendor.integration.pr.base-url:http://localhost:8082}") String prBaseUrl) {
        return builder.baseUrl(prBaseUrl).build();
    }
}
