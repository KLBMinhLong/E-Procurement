package com.eprocure.notification.infrastructure.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class GatewayPrincipalHandshakeHandler extends DefaultHandshakeHandler {
    private static final String USER_ID_HEADER = "X-User-ID";

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Optional<String> gatewayUserId = normalizeUserId(request.getHeaders().getFirst(USER_ID_HEADER));
        if (gatewayUserId.isPresent()) {
            return new GatewayWebSocketPrincipal(gatewayUserId.get());
        }
        Principal requestPrincipal = request.getPrincipal();
        if (requestPrincipal != null) {
            return requestPrincipal;
        }
        return super.determineUser(request, wsHandler, attributes);
    }

    private Optional<String> normalizeUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            return Optional.empty();
        }
        String userId = rawUserId.trim();
        try {
            return Optional.of(UUID.fromString(userId).toString());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
