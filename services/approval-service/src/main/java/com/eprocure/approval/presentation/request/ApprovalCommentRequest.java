package com.eprocure.approval.presentation.request;

import jakarta.validation.constraints.Size;

public record ApprovalCommentRequest(
        @Size(max = 2000) String comment) {
}
