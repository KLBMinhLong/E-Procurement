package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.UploadAttachmentCommand;
import com.eprocure.pr.application.port.out.FileStoragePort;
import com.eprocure.pr.application.service.AttachmentInfoView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.PrAttachment;
import com.eprocure.pr.domain.repository.PrAttachmentRepository;
import com.eprocure.pr.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadAttachmentUseCase {
    private static final Logger log = LogManager.getLogger(UploadAttachmentUseCase.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png"
    );

    private final FileStoragePort fileStoragePort;
    private final PrAttachmentRepository attachmentRepository;

    public UploadAttachmentUseCase(FileStoragePort fileStoragePort, PrAttachmentRepository attachmentRepository) {
        this.fileStoragePort = fileStoragePort;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public AttachmentInfoView execute(UploadAttachmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        log.info("[ACTION] Start UploadAttachment | userId={} | fileName={} | size={}",
                LogMaskingUtil.maskId(command.actorId()), command.fileName(), command.fileSize());

        if (command.fileSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PR_008);
        }

        if (!ALLOWED_MIME_TYPES.contains(command.mimeType())) {
            throw new BusinessException(ErrorCode.PR_009);
        }

        // Store file content
        String filePath = fileStoragePort.storeFile(command.fileName(), command.content(), command.mimeType());

        // Create domain model
        PrAttachment attachment = PrAttachment.create(
                command.fileName(),
                filePath,
                command.fileSize(),
                command.mimeType(),
                command.actorId(),
                Instant.now()
        );

        // Save metadata to DB
        attachmentRepository.save(attachment);

        log.info("[ACTION] Complete UploadAttachment | attachmentId={}", attachment.getId());

        return AttachmentInfoView.from(attachment);
    }
}
