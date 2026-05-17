package com.eprocure.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

final class EprocureIamUserAdapter extends AbstractUserAdapterFederatedStorage {
    private final ComponentModel componentModel;
    private final EprocureUserRepresentation user;

    EprocureIamUserAdapter(
            KeycloakSession session,
            RealmModel realm,
            ComponentModel componentModel,
            EprocureUserRepresentation user) {
        super(session, realm, componentModel);
        this.componentModel = componentModel;
        this.user = user;
    }

    @Override
    public String getId() {
        return StorageId.keycloakId(componentModel, user.id());
    }

    @Override
    public String getUsername() {
        return user.username();
    }

    @Override
    public void setUsername(String username) {
        throw new ReadOnlyException("eProcure IAM users are read-only in Keycloak");
    }

    @Override
    public String getEmail() {
        return user.email();
    }

    @Override
    public String getFirstName() {
        String[] parts = user.fullName().split("\\s+", 2);
        return parts.length == 0 ? "" : parts[0];
    }

    @Override
    public String getLastName() {
        String[] parts = user.fullName().split("\\s+", 2);
        return parts.length < 2 ? "" : parts[1];
    }

    @Override
    public boolean isEnabled() {
        return user.enabled();
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }
}
