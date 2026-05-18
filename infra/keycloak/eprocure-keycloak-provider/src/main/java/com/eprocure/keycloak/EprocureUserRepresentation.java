package com.eprocure.keycloak;

record EprocureUserRepresentation(
        String id,
        String username,
        String email,
        String fullName,
        boolean enabled) {
}
