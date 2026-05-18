# Keycloak Custom Provider Guide

## Common SPIs
- Authenticator SPI for custom login flows.
- User Storage SPI to federate external user stores.
- Event Listener SPI for audit and integration.

## Authenticator SPI Steps
1. Implement Authenticator and AuthenticatorFactory.
2. Register in META-INF/services:
   - org.keycloak.authentication.AuthenticatorFactory
3. Build jar and deploy to Keycloak providers directory.
4. Configure flow in Keycloak Admin Console.

## Authenticator Skeleton
```java
public class CustomAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Validate, then set success or failure
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Handle form actions if needed
    }
}
```

## Factory Skeleton
```java
public class CustomAuthenticatorFactory implements AuthenticatorFactory {
    @Override
    public String getId() { return "custom-auth"; }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new CustomAuthenticator();
    }
}
```

## eProcure implementation

- Provider module: `infra/keycloak/eprocure-keycloak-provider`
- Provider id: `eprocure-iam-user-storage`
- Docker image: `infra/keycloak/Dockerfile` builds the provider jar and installs it into `/opt/keycloak/providers/`
- Realm config: `infra/keycloak/realm-eprocure.json` registers the provider under `org.keycloak.storage.UserStorageProvider`
- IAM provider base URL comes from `IAM_PROVIDER_BASE_URL` and is injected into `realm-eprocure.json` during import.
- Provider HTTP timeout comes from `IAM_PROVIDER_TIMEOUT_SECONDS`; default local sample is 3 seconds.
- Internal auth header: `X-Internal-Api-Key`, value from `IAM_INTERNAL_API_KEY`

## IAM internal endpoints used by the provider

```http
GET /internal/keycloak/users/{userId}
GET /internal/keycloak/users?login={usernameOrEmail}
POST /internal/keycloak/credentials/verify
```

The credential verify endpoint checks IAM `password_hash` with `BCrypt(password + userId)`. Keycloak stores no local eProcure user passwords.

## Recommendations
- Do not store user data in Keycloak DB.
- Implement UserStorageProvider + CredentialInputValidator.
- Call IAM Service to verify credentials (no direct DB access).
- Use short HTTP timeouts and fail closed when IAM is unavailable.
- Map errors to Keycloak error codes for correct UI messages.
- Do not log secrets or raw tokens.
- Use Keycloak config properties or environment variables for external endpoints.
