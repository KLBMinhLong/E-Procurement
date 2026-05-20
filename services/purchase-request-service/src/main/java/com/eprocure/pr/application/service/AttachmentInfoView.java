package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrAttachment;
import java.time.Instant;
import java.util.UUID;

public record AttachmentInfoView(
        UUID id,
        String fileName,
        long fileSize,
        String mimeType,
        Instant uploadedAt
) {
    public static AttachmentInfoView from(PrAttachment attachment) {
        return new AttachmentInfoView(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileSize(),
                attachment.getMimeType(),
                attachment.getUploadedAt()
        );
    }
}
