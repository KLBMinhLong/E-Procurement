## SK-29 · Keycloak Custom Provider

### Trigger
Agent cần triển khai Keycloak custom provider cho IAM.

### Inputs Required
- IAM service client
- Credential types
- User lookup fields

### Rules
```
[R1] Keycloak chỉ làm vai trò xác thực — không lưu user data trong Keycloak DB
[R2] Custom Provider gọi IAM Service để verify credentials
[R3] Provider implement: UserStorageProvider + CredentialInputValidator
[R4] Password verify: so sánh với hash trong DB IAM (BCrypt + userId salt)
[R5] Không sync user từ Keycloak — chỉ look up và validate
```

### Template
```java
public class EprocureUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final IamServiceClient iamClient; // HTTP client gọi IAM Service

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        EprocureUser user = iamClient.findByUsername(username);
        if (user == null) return null;
        return new EprocureUserAdapter(session, realm, model, user);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user,
                           CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;

        String rawPassword = input.getChallengeResponse();
        // Gọi IAM Service verify — không access DB trực tiếp
        return iamClient.verifyPassword(user.getUsername(), rawPassword);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }
}
```

### Checklist
```
[ ] Không sync user vào Keycloak DB
[ ] Verify credentials qua IAM Service
[ ] Implement UserStorageProvider + CredentialInputValidator
```
