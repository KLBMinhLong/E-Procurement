package com.eprocure.approval.application.service;

import java.util.Map;

public record ApprovalTaskDetail(
        ApprovalTaskSummary task,
        ApprovalProcessDetail process,
        Map<String, Object> entitySnapshot) {
}
