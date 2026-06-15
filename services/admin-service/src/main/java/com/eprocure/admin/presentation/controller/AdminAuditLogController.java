package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.ExportAuditLogUseCase;
import com.eprocure.admin.application.usecase.GetAuditExportDownloadUseCase;
import com.eprocure.admin.application.usecase.GetAuditExportJobUseCase;
import com.eprocure.admin.application.usecase.QueryAuditLogUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminAuditPresentationMapper;
import com.eprocure.admin.presentation.request.AuditLogExportRequest;
import com.eprocure.admin.presentation.response.AuditExportJobResponse;
import com.eprocure.admin.presentation.response.AuditLogEntryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-log")
public class AdminAuditLogController {
    private static final Logger log = LogManager.getLogger(AdminAuditLogController.class);

    private final QueryAuditLogUseCase queryAuditLogUseCase;
    private final ExportAuditLogUseCase exportAuditLogUseCase;
    private final GetAuditExportJobUseCase getAuditExportJobUseCase;
    private final GetAuditExportDownloadUseCase getAuditExportDownloadUseCase;
    private final AdminAuditPresentationMapper mapper;

    public AdminAuditLogController(
            QueryAuditLogUseCase queryAuditLogUseCase,
            ExportAuditLogUseCase exportAuditLogUseCase,
            GetAuditExportJobUseCase getAuditExportJobUseCase,
            GetAuditExportDownloadUseCase getAuditExportDownloadUseCase,
            AdminAuditPresentationMapper mapper) {
        this.queryAuditLogUseCase = queryAuditLogUseCase;
        this.exportAuditLogUseCase = exportAuditLogUseCase;
        this.getAuditExportJobUseCase = getAuditExportJobUseCase;
        this.getAuditExportDownloadUseCase = getAuditExportDownloadUseCase;
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

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('SYSTEM_AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<AuditExportJobResponse>> exportAuditLog(
            @Valid @RequestBody AuditLogExportRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/admin/audit-log/export | userId={} | from={} | to={}",
                LogMaskingUtil.maskId(principal.getId()),
                body.fromTime(),
                body.toTime());
        var result = exportAuditLogUseCase.execute(mapper.toCommand(principal, body), idempotencyKey);
        ResponseEntity.BodyBuilder builder = result.replayed()
                ? ResponseEntity.ok()
                : ResponseEntity.status(HttpStatus.ACCEPTED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.job()), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/export/{jobId}")
    @PreAuthorize("hasAuthority('SYSTEM_AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<AuditExportJobResponse>> getExportJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/audit-log/export/{} | userId={}",
                LogMaskingUtil.maskId(jobId),
                LogMaskingUtil.maskId(principal.getId()));
        var job = getAuditExportJobUseCase.execute(mapper.toQuery(principal, jobId));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(job), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/export/{jobId}/download")
    @PreAuthorize("hasAuthority('SYSTEM_AUDIT_VIEW')")
    public ResponseEntity<Resource> downloadExport(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[CONTROLLER] GET /api/v1/admin/audit-log/export/{}/download | userId={}",
                LogMaskingUtil.maskId(jobId),
                LogMaskingUtil.maskId(principal.getId()));
        var job = getAuditExportDownloadUseCase.execute(mapper.toQuery(principal, jobId));
        Path storagePath = Path.of(job.storagePath().orElseThrow(
                () -> new BusinessException(ErrorCode.AUDIT_EXPORT_FILE_NOT_READY)));
        if (!Files.isRegularFile(storagePath)) {
            throw new BusinessException(ErrorCode.AUDIT_EXPORT_FILE_NOT_READY);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName(job.fileName().orElse(storagePath.getFileName().toString())) + "\"")
                .body(new FileSystemResource(storagePath));
    }

    private String safeFileName(String fileName) {
        return fileName.replace("\"", "");
    }
}
