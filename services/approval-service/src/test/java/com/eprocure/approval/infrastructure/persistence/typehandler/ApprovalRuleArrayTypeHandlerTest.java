package com.eprocure.approval.infrastructure.persistence.typehandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class ApprovalRuleArrayTypeHandlerTest {

    @Test
    void should_bind_uuid_array_with_postgres_uuid_type_when_setting_parameter() throws Exception {
        UuidArrayTypeHandler handler = new UuidArrayTypeHandler();
        JdbcArrayCapture capture = new JdbcArrayCapture();
        UUID firstId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID secondId = UUID.fromString("22222222-2222-4222-8222-222222222222");

        handler.setNonNullParameter(
                preparedStatement(capture),
                8,
                new UUID[] { firstId, secondId },
                JdbcType.ARRAY);

        assertThat(capture.createdArrayType).isEqualTo("uuid");
        assertThat(capture.createdArrayValues).containsExactly(firstId, secondId);
        assertThat(capture.boundParameterIndex).isEqualTo(8);
        assertThat(capture.boundArray).isSameAs(capture.sqlArray);
    }

    @Test
    void should_bind_string_array_with_postgres_varchar_type_when_setting_parameter() throws Exception {
        StringArrayTypeHandler handler = new StringArrayTypeHandler();
        JdbcArrayCapture capture = new JdbcArrayCapture();

        handler.setNonNullParameter(
                preparedStatement(capture),
                7,
                new String[] { "SOFTWARE", "SAAS" },
                JdbcType.ARRAY);

        assertThat(capture.createdArrayType).isEqualTo("varchar");
        assertThat(capture.createdArrayValues).containsExactly("SOFTWARE", "SAAS");
        assertThat(capture.boundParameterIndex).isEqualTo(7);
        assertThat(capture.boundArray).isSameAs(capture.sqlArray);
    }

    private PreparedStatement preparedStatement(JdbcArrayCapture capture) {
        Connection connection = connection(capture);
        return (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { PreparedStatement.class },
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        return connection;
                    }
                    if ("setArray".equals(method.getName())) {
                        capture.boundParameterIndex = (Integer) args[0];
                        capture.boundArray = (Array) args[1];
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Connection connection(JdbcArrayCapture capture) {
        return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("createArrayOf".equals(method.getName())) {
                        capture.createdArrayType = (String) args[0];
                        capture.createdArrayValues = (Object[]) args[1];
                        return capture.sqlArray;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || Void.TYPE.equals(returnType)) {
            return null;
        }
        if (Boolean.TYPE.equals(returnType)) {
            return false;
        }
        if (Character.TYPE.equals(returnType)) {
            return '\0';
        }
        return 0;
    }

    private final class JdbcArrayCapture {
        private final Array sqlArray = (Array) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Array.class },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        private String createdArrayType;
        private Object[] createdArrayValues;
        private int boundParameterIndex;
        private Array boundArray;
    }
}
