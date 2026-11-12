package com.quickbite.administration.dto;

import com.quickbite.administration.Admin;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.VerificationStatus;

import java.time.Instant;

public record AdminAccountResponse(
        Long id,
        Long identityId,
        String email,
        String displayName,
        AccountStatus accountStatus,
        VerificationStatus verificationStatus,
        String requestNote,
        Instant createdAt
) {
    public static AdminAccountResponse from(Admin admin) {
        return new AdminAccountResponse(
                admin.getId(),
                admin.getIdentity().getId(),
                admin.getIdentity().getEmail(),
                admin.getDisplayName(),
                admin.getAccountStatus(),
                admin.getVerificationStatus(),
                admin.getRequestNote(),
                admin.getCreatedAt()
        );
    }
}

