package com.eprocure.keycloak;

import java.time.Duration;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

public final class EprocureIamUserStorageProviderFactory
        implements UserStorageProviderFactory<EprocureIamUserStorageProvider> {
    public static final String PROVIDER_ID = "eprocure-iam-user-storage";
    private static final String CFG_IAM_BASE_URL = "iamBaseUrl";
    private static final String CFG_INTERNAL_API_KEY = "internalApiKey";
    private static final String CFG_TIMEOUT_SECONDS = "timeoutSeconds";
    private static final String ENV_IAM_PROVIDER_BASE_URL = "IAM_PROVIDER_BASE_URL";
    private static final String ENV_IAM_INTERNAL_API_KEY = "IAM_INTERNAL_API_KEY";
    private static final String ENV_IAM_PROVIDER_TIMEOUT_SECONDS = "IAM_PROVIDER_TIMEOUT_SECONDS";
    private static final String DEFAULT_TIMEOUT_SECONDS = "3";

    @Override
    public EprocureIamUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        String iamBaseUrl = requiredConfig(model, CFG_IAM_BASE_URL, ENV_IAM_PROVIDER_BASE_URL);
        String internalApiKey = configValue(model, CFG_INTERNAL_API_KEY, env(ENV_IAM_INTERNAL_API_KEY, ""));
        long timeoutSeconds = parsePositiveLong(configValue(
                model,
                CFG_TIMEOUT_SECONDS,
                env(ENV_IAM_PROVIDER_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)));
        EprocureIamClient client = new EprocureIamClient(
                iamBaseUrl,
                internalApiKey,
                Duration.ofSeconds(timeoutSeconds));
        return new EprocureIamUserStorageProvider(session, model, client);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Federates eProcure IAM users and validates credentials through IAM internal APIs.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty iamBaseUrl = new ProviderConfigProperty(
                CFG_IAM_BASE_URL,
                "IAM base URL",
                "Base URL for IAM internal Keycloak provider APIs.",
                ProviderConfigProperty.STRING_TYPE,
                env(ENV_IAM_PROVIDER_BASE_URL, ""));
        ProviderConfigProperty internalApiKey = new ProviderConfigProperty(
                CFG_INTERNAL_API_KEY,
                "Internal API key",
                "Shared API key sent to IAM internal endpoints. Prefer environment variable IAM_INTERNAL_API_KEY.",
                ProviderConfigProperty.PASSWORD,
                "");
        ProviderConfigProperty timeoutSeconds = new ProviderConfigProperty(
                CFG_TIMEOUT_SECONDS,
                "Timeout seconds",
                "HTTP timeout for IAM internal API calls.",
                ProviderConfigProperty.STRING_TYPE,
                env(ENV_IAM_PROVIDER_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS));
        return List.of(iamBaseUrl, internalApiKey, timeoutSeconds);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    private String configValue(ComponentModel model, String key, String fallback) {
        String value = resolveEnvPlaceholder(model.getConfig().getFirst(key));
        return value == null || value.isBlank() ? fallback : value;
    }

    private String requiredConfig(ComponentModel model, String key, String envKey) {
        String value = configValue(model, key, env(envKey, ""));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " must be configured through component config or " + envKey);
        }
        return value;
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private long parsePositiveLong(String value) {
        long parsed = Long.parseLong(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(CFG_TIMEOUT_SECONDS + " must be greater than zero");
        }
        return parsed;
    }

    private String resolveEnvPlaceholder(String value) {
        if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String key = value.substring(2, value.length() - 1);
        return env(key, "");
    }
}
