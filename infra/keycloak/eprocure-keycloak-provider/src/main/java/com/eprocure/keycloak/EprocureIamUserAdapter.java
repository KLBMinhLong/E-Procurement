package com.eprocure.keycloak;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        String[] parts = fullNameParts();
        return parts.length == 0 ? "" : parts[0];
    }

    @Override
    public String getLastName() {
        String[] parts = fullNameParts();
        return parts.length < 2 ? "" : parts[1];
    }

    @Override
    public String getFirstAttribute(String name) {
        if ("username".equals(name)) {
            return getUsername();
        }
        if ("firstName".equals(name) || FIRST_NAME_ATTRIBUTE.equals(name)) {
            return getFirstName();
        }
        if ("lastName".equals(name) || LAST_NAME_ATTRIBUTE.equals(name)) {
            return getLastName();
        }
        if ("email".equals(name) || EMAIL_ATTRIBUTE.equals(name)) {
            return getEmail();
        }
        if ("emailVerified".equals(name) || EMAIL_VERIFIED_ATTRIBUTE.equals(name)) {
            return Boolean.TRUE.toString();
        }
        return super.getFirstAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attributes = new LinkedHashMap<>(super.getAttributes());
        attributes.put("username", List.of(getUsername()));
        attributes.put("firstName", List.of(getFirstName()));
        attributes.put("lastName", List.of(getLastName()));
        attributes.put("email", List.of(getEmail()));
        attributes.put("emailVerified", List.of(Boolean.TRUE.toString()));
        attributes.put(FIRST_NAME_ATTRIBUTE, List.of(getFirstName()));
        attributes.put(LAST_NAME_ATTRIBUTE, List.of(getLastName()));
        attributes.put(EMAIL_ATTRIBUTE, List.of(getEmail()));
        attributes.put(EMAIL_VERIFIED_ATTRIBUTE, List.of(Boolean.TRUE.toString()));
        return attributes;
    }

    @Override
    public boolean isEnabled() {
        return user.enabled();
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }

    private String[] fullNameParts() {
        String fullName = user.fullName() == null || user.fullName().isBlank()
                ? user.username()
                : user.fullName().trim();
        return fullName.split("\\s+", 2);
    }
}
