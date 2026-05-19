package com.eprocure.pr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableTransactionManagement
public class PurchaseRequestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PurchaseRequestServiceApplication.class, args);
    }
}
