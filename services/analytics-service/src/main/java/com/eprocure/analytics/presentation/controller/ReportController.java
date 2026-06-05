package com.eprocure.analytics.presentation.controller;

import com.eprocure.analytics.application.service.ReportJobMutationResult;
import com.eprocure.analytics.application.usecase.ExportReportUseCase;
import com.eprocure.analytics.application.usecase.GetReportDownloadUseCase;
import com.eprocure.analytics.application.usecase.GetReportJobUseCase;
import com.eprocure.analytics.common.api.ApiResponse;
import com.eprocure.analytics.common.api.RequestIdUtil;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.presentation.mapper.ReportPresentationMapper;
import com.eprocure.analytics.presentation.request.ExportReportRequest;
import com.eprocure.analytics.presentation.response.ReportJobResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private static final Logger log = LogManager.getLogger(ReportController.class);

    private final ExportReportUseCase exportReportUseCase;
    private final GetReportJobUseCase getReportJobUseCase;
    private final GetReportDownloadUseCase getReportDownloadUseCase;
    private final ReportPresentationMapper mapper;

    public ReportController(
            ExportReportUseCase exportReportUseCase,
            GetReportJobUseCase getReportJobUseCase,
            GetReportDownloadUseCase getReportDownloadUseCase,
            ReportPresentationMapper mapper) {
        this.exportReportUseCase = exportReportUseCase;
        this.getReportJobUseCase = getReportJobUseCase;
        this.getReportDownloadUseCase = getReportDownloadUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<ApiResponse<ReportJobResponse>> export(
            @Valid @RequestBody ExportReportRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/reports/export | userId={} | reportType={} | format={}",
                LogMaskingUtil.maskId(principal.getId()),
                body.reportType(),
                body.format());
        ReportJobMutationResult result = exportReportUseCase.execute(
                mapper.toCommand(principal, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = result.replayed()
                ? ResponseEntity.ok()
                : ResponseEntity.status(HttpStatus.ACCEPTED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.job()), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<ApiResponse<ReportJobResponse>> getJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/reports/jobs/{} | userId={}",
                LogMaskingUtil.maskId(jobId),
                LogMaskingUtil.maskId(principal.getId()));
        var job = getReportJobUseCase.execute(mapper.toQuery(principal, jobId));
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(job), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/jobs/{jobId}/download")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<Resource> download(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[CONTROLLER] GET /api/v1/reports/jobs/{}/download | userId={}",
                LogMaskingUtil.maskId(jobId),
                LogMaskingUtil.maskId(principal.getId()));
        var job = getReportDownloadUseCase.execute(mapper.toQuery(principal, jobId));
        Path storagePath = Path.of(job.storagePath());
        if (!Files.isRegularFile(storagePath)) {
            throw new BusinessException(ErrorCode.ANL_003);
        }
        return ResponseEntity.ok()
                .contentType(mediaType(job.format()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + storagePath.getFileName() + "\"")
                .body(new FileSystemResource(storagePath));
    }

    private MediaType mediaType(ReportFormat format) {
        if (format == ReportFormat.PDF) {
            return MediaType.APPLICATION_PDF;
        }
        return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
}
