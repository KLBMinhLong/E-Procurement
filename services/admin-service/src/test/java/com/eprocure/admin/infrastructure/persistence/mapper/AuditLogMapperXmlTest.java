package com.eprocure.admin.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AuditLogMapperXmlTest {

    @Test
    @DisplayName("AuditLog MyBatis XML parse được khi SqlSessionFactory khởi tạo")
    void should_parse_audit_log_mapper_xml_when_sql_session_factory_starts() {
        var factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/db_audit",
                "audit_user",
                "audit_pass_dev"));
        factoryBean.setTypeHandlersPackage("com.eprocure.admin.infrastructure.persistence.typehandler");

        assertThatCode(() -> {
            var resolver = new PathMatchingResourcePatternResolver();
            factoryBean.setMapperLocations(resolver.getResources("classpath:mapper/AuditLogMapper.xml"));
            factoryBean.afterPropertiesSet();
        }).doesNotThrowAnyException();
    }
}
