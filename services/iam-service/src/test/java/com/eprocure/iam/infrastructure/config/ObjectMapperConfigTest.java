package com.eprocure.iam.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.iam.presentation.auth.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ObjectMapperConfigTest {

    @Test
    void should_deserialize_api_record_with_primary_object_mapper() throws Exception {
        ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

        LoginRequest request = objectMapper.readValue(
                """
                {"username":"requester","password":"Password@123"}
                """,
                LoginRequest.class);

        assertThat(request.username()).isEqualTo("requester");
        assertThat(request.password()).isEqualTo("Password@123");
    }
}
