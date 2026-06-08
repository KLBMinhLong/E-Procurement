package com.eprocure.approval.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient iamRestClient(
            RestClient.Builder builder,
            @Value("${eprocure.integration.iam.base-url:http://localhost:8081}") String iamBaseUrl) {
        return patchCapable(builder).baseUrl(iamBaseUrl).build();
    }

    @Bean
    public RestClient purchaseRequestRestClient(
            RestClient.Builder builder,
            @Value("${eprocure.integration.pr.base-url:http://localhost:8082}") String prBaseUrl) {
        return patchCapable(builder).baseUrl(prBaseUrl).build();
    }

    private RestClient.Builder patchCapable(RestClient.Builder builder) {
        return builder.requestFactory(new JdkClientHttpRequestFactory());
    }
}
