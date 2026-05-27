package com.eprocure.approval.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class ApprovalStepDbEntity {
    private UUID id;
    private UUID processId;
    private int stepIndex;
    private String stepType;
    private String approverRole;
    private UUID approverId;
    private UUID delegateId;
    private String status;
    private Instant slaDeadline;
    private Instant assignedAt;
    private String camundaTaskId;
    private UUID createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public UUID getDelegateId() {
        return delegateId;
    }

    public void setDelegateId(UUID delegateId) {
        this.delegateId = delegateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(Instant slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getCamundaTaskId() {
        return camundaTaskId;
    }

    public void setCamundaTaskId(String camundaTaskId) {
        this.camundaTaskId = camundaTaskId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
