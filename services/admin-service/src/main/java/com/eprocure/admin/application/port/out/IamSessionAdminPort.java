package com.eprocure.admin.application.port.out;

import com.eprocure.admin.application.service.ActiveSessionView;
import com.eprocure.admin.application.service.PageResult;
import java.util.UUID;

public interface IamSessionAdminPort {
    PageResult<ActiveSessionView> listActiveSessions(UUID userId, int page, int size);

    void invalidateSession(UUID sessionId, UUID actorId, String reason, UUID idempotencyKey);
}
