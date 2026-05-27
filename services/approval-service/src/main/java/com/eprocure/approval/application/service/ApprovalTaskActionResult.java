package com.eprocure.approval.application.service;

public record ApprovalTaskActionResult(
        ApprovalTaskActionView view,
        boolean replayed) {

    public static ApprovalTaskActionResult fresh(ApprovalTaskActionView view) {
        return new ApprovalTaskActionResult(view, false);
    }

    public static ApprovalTaskActionResult replayed(ApprovalTaskActionView view) {
        return new ApprovalTaskActionResult(view, true);
    }
}
