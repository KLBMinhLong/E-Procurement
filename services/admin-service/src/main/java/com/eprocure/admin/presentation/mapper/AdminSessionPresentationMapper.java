package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.InvalidateSessionCommand;
import com.eprocure.admin.application.port.in.ListActiveSessionsQuery;
import com.eprocure.admin.application.service.ActiveSessionView;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.request.InvalidateSessionRequest;
import com.eprocure.admin.presentation.response.ActiveSessionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminSessionPresentationMapper {

    public ListActiveSessionsQuery toQuery(UUID userId, Integer page, Integer size) {
        return new ListActiveSessionsQuery(
                page == null ? 1 : page,
                size == null ? 50 : size,
                userId);
    }

    public InvalidateSessionCommand toCommand(UUID sessionId, InvalidateSessionRequest request, UserPrincipal principal) {
        return new InvalidateSessionCommand(sessionId, principal.getId(), request.reason());
    }

    public List<ActiveSessionResponse> toResponseList(List<ActiveSessionView> sessions) {
        return sessions.stream().map(this::toResponse).toList();
    }

    private ActiveSessionResponse toResponse(ActiveSessionView session) {
        return new ActiveSessionResponse(
                session.sessionId(),
                session.userId(),
                session.userName(),
                session.fullName(),
                session.ipAddress(),
                session.userAgent(),
                session.createdAt(),
                session.lastActivity(),
                session.expiresAt());
    }
}
