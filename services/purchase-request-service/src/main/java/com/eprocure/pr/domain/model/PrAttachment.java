package com.eprocure.pr.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PrAttachment {
    private UUID id;
    private UUID prId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private UUID uploadedBy;
    private Instant uploadedAt;
    private boolean deleted;
    private Instant deletedAt;

    private PrAttachment() {
    }

    private PrAttachment(
            UUID id,
            UUID prId,
            String fileName,
            String filePath,
            long fileSize,
            String mimeType,
            UUID uploadedBy,
            Instant uploadedAt,
            boolean deleted,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.prId = prId;
        this.fileName = Objects.requireNonNull(fileName, "fileName must not be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.fileSize = fileSize;
        this.mimeType = Objects.requireNonNull(mimeType, "mimeType must not be null");
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "uploadedBy must not be null");
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    public static PrAttachment create(
            String fileName,
            String filePath,
            long fileSize,
            String mimeType,
            UUID uploadedBy,
            Instant uploadedAt) {
        return new PrAttachment(
                UUID.randomUUID(),
                null,
                fileName,
                filePath,
                fileSize,
                mimeType,
                uploadedBy,
                uploadedAt,
                false,
                null
        );
    }

    public static PrAttachment reconstitute(
            UUID id,
            UUID prId,
            String fileName,
            String filePath,
            long fileSize,
            String mimeType,
            UUID uploadedBy,
            Instant uploadedAt,
            boolean deleted,
            Instant deletedAt) {
        return new PrAttachment(
                id, prId, fileName, filePath, fileSize, mimeType, uploadedBy, uploadedAt, deleted, deletedAt
        );
    }

    public void attachToPr(UUID prId) {
        this.prId = Objects.requireNonNull(prId, "prId must not be null");
    }

    public UUID getId() {
        return id;
    }

    public Optional<UUID> getPrId() {
        return Optional.ofNullable(prId);
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }
}
