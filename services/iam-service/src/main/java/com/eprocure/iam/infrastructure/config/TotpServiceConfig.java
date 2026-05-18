package com.eprocure.iam.infrastructure.config;

import com.eprocure.iam.application.service.TotpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotpServiceConfig {

    @Bean
    public TotpService totpService(@Value("${eprocure.two-factor.issuer:eProcure}") String issuer) {
        return new TotpService(issuer);
    }
}
