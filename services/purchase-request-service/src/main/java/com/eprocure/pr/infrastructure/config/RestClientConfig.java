package com.eprocure.pr.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient financeRestClient(
            RestClient.Builder builder,
            @Value("${eprocure.pr.integration.finance.base-url:http://localhost:8084}") String financeBaseUrl) {
        return builder.baseUrl(financeBaseUrl).build();
    }
}
