package com.eprocure.notification.infrastructure.websocket;

import java.security.Principal;
import java.util.Objects;

public record GatewayWebSocketPrincipal(String name) implements Principal {
    public GatewayWebSocketPrincipal {
        name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return name;
    }
}
