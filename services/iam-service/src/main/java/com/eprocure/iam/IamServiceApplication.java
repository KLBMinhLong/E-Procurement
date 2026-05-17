package com.eprocure.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
