package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.dashboard.SlaWarning;
import java.time.Instant;

public class ManagerSlaWarningDbEntity {
    private String approvalStepId;
    private String prNumber;
    private Instant slaDeadline;
    private boolean overdue;

    public SlaWarning toDomain() {
        return new SlaWarning(
                approvalStepId == null ? "" : approvalStepId,
                prNumber,
                slaDeadline,
                overdue);
    }

    public String getApprovalStepId() {
        return approvalStepId;
    }

    public void setApprovalStepId(String approvalStepId) {
        this.approvalStepId = approvalStepId;
    }

    public String getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(String prNumber) {
        this.prNumber = prNumber;
    }

    public Instant getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(Instant slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }
}
