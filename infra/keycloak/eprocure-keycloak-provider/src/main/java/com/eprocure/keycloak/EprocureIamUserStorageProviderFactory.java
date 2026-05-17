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

    @Override
    public EprocureIamUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        String iamBaseUrl = configValue(model, CFG_IAM_BASE_URL, env("IAM_PROVIDER_BASE_URL", "http://iam-service:8081"));
        String internalApiKey = configValue(model, CFG_INTERNAL_API_KEY, env("IAM_INTERNAL_API_KEY", ""));
        long timeoutSeconds = Long.parseLong(configValue(model, CFG_TIMEOUT_SECONDS, "5"));
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
                env("IAM_PROVIDER_BASE_URL", "http://iam-service:8081"));
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
                "5");
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
        String value = model.getConfig().getFirst(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
