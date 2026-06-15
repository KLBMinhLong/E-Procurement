package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.QueryAuditLogUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminAuditPresentationMapper;
import com.eprocure.admin.presentation.response.AuditLogEntryResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-log")
public class AdminAuditLogController {
    private static final Logger log = LogManager.getLogger(AdminAuditLogController.class);

    private final QueryAuditLogUseCase queryAuditLogUseCase;
    private final AdminAuditPresentationMapper mapper;

    public AdminAuditLogController(
            QueryAuditLogUseCase queryAuditLogUseCase,
            AdminAuditPresentationMapper mapper) {
        this.queryAuditLogUseCase = queryAuditLogUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<List<AuditLogEntryResponse>>> queryAuditLog(
            @RequestParam(value = "actor_id", required = false) UUID actorId,
            @RequestParam(value = "entity_type", required = false) String entityType,
            @RequestParam(value = "entity_id", required = false) UUID entityId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "service_name", required = false) String serviceName,
            @RequestParam(value = "is_success", required = false) Boolean success,
            @RequestParam("from_time") Instant fromTime,
            @RequestParam("to_time") Instant toTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/audit-log | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        var filter = mapper.toFilter(
                actorId,
                entityType,
                entityId,
                action,
                serviceName,
                success,
                fromTime,
                toTime,
                page,
                size);
        var result = queryAuditLogUseCase.execute(filter, principal.getId());
        return ResponseEntity.ok(ApiResponse.successWithMeta(
                mapper.toResponseList(result.items()),
                result.meta(),
                RequestIdUtil.resolve(request)));
    }
}
