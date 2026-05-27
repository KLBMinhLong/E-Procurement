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
        // Environment variables ALWAYS take priority over component config (DB).
        // This allows switching between local/Docker by only changing .env
        // and restarting Keycloak — no DB updates needed.
        String iamBaseUrl = envFirst(ENV_IAM_PROVIDER_BASE_URL,
                model, CFG_IAM_BASE_URL, null);
        if (iamBaseUrl == null || iamBaseUrl.isBlank()) {
            throw new IllegalStateException(CFG_IAM_BASE_URL
                    + " must be configured through " + ENV_IAM_PROVIDER_BASE_URL
                    + " environment variable or component config");
        }
        String internalApiKey = envFirst(ENV_IAM_INTERNAL_API_KEY,
                model, CFG_INTERNAL_API_KEY, "");
        long timeoutSeconds = parsePositiveLong(
                envFirst(ENV_IAM_PROVIDER_TIMEOUT_SECONDS,
                        model, CFG_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS));
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
                "Base URL for IAM internal Keycloak provider APIs. "
                        + "Environment variable " + ENV_IAM_PROVIDER_BASE_URL + " always takes priority.",
                ProviderConfigProperty.STRING_TYPE,
                env(ENV_IAM_PROVIDER_BASE_URL, ""));
        ProviderConfigProperty internalApiKey = new ProviderConfigProperty(
                CFG_INTERNAL_API_KEY,
                "Internal API key",
                "Shared API key sent to IAM internal endpoints. "
                        + "Environment variable " + ENV_IAM_INTERNAL_API_KEY + " always takes priority.",
                ProviderConfigProperty.PASSWORD,
                "");
        ProviderConfigProperty timeoutSeconds = new ProviderConfigProperty(
                CFG_TIMEOUT_SECONDS,
                "Timeout seconds",
                "HTTP timeout for IAM internal API calls. "
                        + "Environment variable " + ENV_IAM_PROVIDER_TIMEOUT_SECONDS + " always takes priority.",
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

    /**
     * Resolves config with env-var-first priority:
     *   1. Environment variable (always wins if set)
     *   2. Component model config from DB
     *   3. Fallback default
     */
    private String envFirst(String envKey, ComponentModel model, String configKey, String fallback) {
        String envValue = env(envKey, null);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String modelValue = resolveEnvPlaceholder(model.getConfig().getFirst(configKey));
        if (modelValue != null && !modelValue.isBlank()) {
            return modelValue;
        }
        return fallback;
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
