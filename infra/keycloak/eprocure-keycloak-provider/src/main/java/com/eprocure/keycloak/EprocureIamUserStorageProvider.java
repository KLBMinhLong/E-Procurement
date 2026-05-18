package com.eprocure.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

public final class EprocureIamUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {
    private final KeycloakSession session;
    private final ComponentModel componentModel;
    private final EprocureIamClient iamClient;

    EprocureIamUserStorageProvider(
            KeycloakSession session,
            ComponentModel componentModel,
            EprocureIamClient iamClient) {
        this.session = session;
        this.componentModel = componentModel;
        this.iamClient = iamClient;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        // Keycloak SPI requires null when a federated user is not found.
        return iamClient.findById(externalId)
                .map(user -> toUserModel(realm, user))
                .orElse(null);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return iamClient.findByLogin(username)
                .map(user -> toUserModel(realm, user))
                .orElse(null);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return iamClient.findByLogin(email)
                .map(user -> toUserModel(realm, user))
                .orElse(null);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel credential)) {
            return false;
        }
        return iamClient.verifyPassword(user.getUsername(), credential.getChallengeResponse());
    }

    @Override
    public void close() {
    }

    private UserModel toUserModel(RealmModel realm, EprocureUserRepresentation user) {
        return new EprocureIamUserAdapter(session, realm, componentModel, user);
    }
}
