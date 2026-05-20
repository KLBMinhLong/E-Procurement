package com.eprocure.pr.presentation.controller;

import com.eprocure.pr.application.port.in.UploadAttachmentCommand;
import com.eprocure.pr.application.service.AttachmentInfoView;
import com.eprocure.pr.application.usecase.UploadAttachmentUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/purchase-requests/attachments")
public class AttachmentController {
    private static final Logger log = LogManager.getLogger(AttachmentController.class);

    private final UploadAttachmentUseCase uploadAttachmentUseCase;

    public AttachmentController(UploadAttachmentUseCase uploadAttachmentUseCase) {
        this.uploadAttachmentUseCase = uploadAttachmentUseCase;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('PR_CREATE')")
    public ResponseEntity<ApiResponse<AttachmentInfoView>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] POST /api/v1/purchase-requests/attachments/upload | userId={} | fileName={}",
                LogMaskingUtil.maskId(principal.getId()), file.getOriginalFilename());

        try {
            UploadAttachmentCommand command = new UploadAttachmentCommand(
                    principal.getId(),
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            AttachmentInfoView result = uploadAttachmentUseCase.execute(command);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(result, RequestIdUtil.resolve(request)));

        } catch (IOException e) {
            log.error("[CONTROLLER] Failed to read uploaded file", e);
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }
}
