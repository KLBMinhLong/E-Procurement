package com.eprocure.pr.application.port.in;

import java.io.InputStream;
import java.util.UUID;

public record UploadAttachmentCommand(
        UUID actorId,
        String fileName,
        InputStream content,
        long fileSize,
        String mimeType
) {
}
