package com.eprocure.admin.infrastructure.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eprocure.admin")
public class AdminProperties {
    private Duration healthTimeout = Duration.ofSeconds(1);
    private List<ServiceEntry> services = new ArrayList<>();
    private Infrastructure infrastructure = new Infrastructure();

    public Duration getHealthTimeout() {
        return healthTimeout;
    }

    public void setHealthTimeout(Duration healthTimeout) {
        this.healthTimeout = healthTimeout;
    }

    public List<ServiceEntry> getServices() {
        return services;
    }

    public void setServices(List<ServiceEntry> services) {
        this.services = services;
    }

    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    public static class ServiceEntry {
        private String name = "";
        private String displayName = "";
        private String baseUrl = "";
        private String version = "";
        private List<VariableEntry> variables = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<VariableEntry> getVariables() {
            return variables;
        }

        public void setVariables(List<VariableEntry> variables) {
            this.variables = variables;
        }
    }

    public static class VariableEntry {
        private String key = "";
        private String value = "";
        private boolean sensitive;
        private String description = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isSensitive() {
            return sensitive;
        }

        public void setSensitive(boolean sensitive) {
            this.sensitive = sensitive;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class Infrastructure {
        private Component postgresql = new Component("postgresql", "localhost", 5432);
        private Component redis = new Component("redis", "localhost", 6379);
        private Component kafka = new Component("kafka", "localhost", 19092);

        public Component getPostgresql() {
            return postgresql;
        }

        public void setPostgresql(Component postgresql) {
            this.postgresql = postgresql;
        }

        public Component getRedis() {
            return redis;
        }

        public void setRedis(Component redis) {
            this.redis = redis;
        }

        public Component getKafka() {
            return kafka;
        }

        public void setKafka(Component kafka) {
            this.kafka = kafka;
        }
    }

    public static class Component {
        private String name = "";
        private String host = "";
        private int port;

        public Component() {
        }

        public Component(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
