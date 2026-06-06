package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportFilterCriteria;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import java.time.Instant;
import java.util.UUID;

public class ReportJobDbEntity {
    private UUID id;
    private String reportType;
    private String format;
    private String status;
    private String downloadUrl;
    private String storagePath;
    private String failureReason;
    private String filtersJson;
    private Instant createdAt;
    private Instant completedAt;
    private Instant expiresAt;
    private UUID createdBy;
    private UUID idempotencyKey;

    public ReportJob toDomain(ReportFilterCriteria filterCriteria) {
        return new ReportJob(
                id,
                ReportType.valueOf(reportType),
                ReportFormat.valueOf(format),
                ReportJobStatus.valueOf(status),
                downloadUrl,
                storagePath,
                failureReason,
                createdAt,
                completedAt,
                expiresAt,
                createdBy,
                idempotencyKey,
                filterCriteria);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getFiltersJson() {
        return filtersJson;
    }

    public void setFiltersJson(String filtersJson) {
        this.filtersJson = filtersJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
