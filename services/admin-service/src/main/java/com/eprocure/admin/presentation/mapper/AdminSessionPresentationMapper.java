package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.InvalidateSessionCommand;
import com.eprocure.admin.application.port.in.ListActiveSessionsQuery;
import com.eprocure.admin.application.service.ActiveSessionView;
import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.request.InvalidateSessionRequest;
import com.eprocure.admin.presentation.response.ActiveSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
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

    public InvalidateSessionCommand toCommand(
            UUID sessionId,
            InvalidateSessionRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return new InvalidateSessionCommand(sessionId, principal.getId(), request.reason(), auditContext);
    }

    public AdminAuditContext toAuditContext(UserPrincipal principal, HttpServletRequest request, String requestId) {
        return new AdminAuditContext(
                principal.getId(),
                principal.getFullName(),
                principal.getPermissions().stream().sorted().toList(),
                Optional.ofNullable(request.getRemoteAddr()),
                Optional.ofNullable(request.getMethod()),
                Optional.ofNullable(request.getRequestURI()),
                Optional.ofNullable(requestId));
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
