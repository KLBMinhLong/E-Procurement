package com.eprocure.admin.infrastructure.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClientCustomizer patchCapableRestClientCustomizer() {
        return builder -> builder.requestFactory(new JdkClientHttpRequestFactory());
    }
}
