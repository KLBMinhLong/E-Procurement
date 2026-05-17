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

## Recommendations
- Do not store user data in Keycloak DB.
- Implement UserStorageProvider + CredentialInputValidator.
- Call IAM Service to verify credentials (no direct DB access).
- Avoid blocking I/O inside auth flow.
- Map errors to Keycloak error codes for correct UI messages.
- Do not log secrets or raw tokens.
- Use Keycloak config properties for external endpoints.
