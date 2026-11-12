package com.quickbite.administration.dto;

import com.quickbite.administration.AdminRoleRequest;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;

import java.time.Instant;

public record AdminRoleRequestResponse(
        Long id,
        Long identityId,
        String email,
        String displayName,
        AccountTypeSnapshot currentRole,
        String reason,
        VerificationStatus status,
        String reviewNote,
        Instant createdAt,
        Instant reviewedAt
) {
    public static AdminRoleRequestResponse from(AdminRoleRequest req, String displayName) {
        return new AdminRoleRequestResponse(
                req.getId(),
                req.getIdentity().getId(),
                req.getIdentity().getEmail(),
                displayName,
                req.getCurrentRole(),
                req.getReason(),
                req.getStatus(),
                req.getReviewNote(),
                req.getCreatedAt(),
                req.getReviewedAt()
        );
    }
}

