package com.eprocure.approval.application.service;

public record ApprovalInboxCount(
        long total,
        long overdue,
        long emergency) {
}
