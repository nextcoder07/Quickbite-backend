package com.quickbite.accountdeletion.dto;

import com.quickbite.accountdeletion.AccountDeletionRequest;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.DeletionRequestStatus;
import java.time.Instant;

public record DeletionRequestResponse(
        Long id,
        Long requesterIdentityId,
        String requesterEmail,
        AccountTypeSnapshot requesterType,
        DeletionRequestStatus status,
        String reason,
        String reviewNote,
        Instant requestedAt,
        Instant reviewedAt,
        Instant completedAt
) {
    public static DeletionRequestResponse from(AccountDeletionRequest req) {
        return new DeletionRequestResponse(
                req.getId(),
                req.getRequester().getId(),
                req.getRequester().getEmail(),
                req.getRequesterType(),
                req.getStatus(),
                req.getReason(),
                req.getReviewNote(),
                req.getRequestedAt(),
                req.getReviewedAt(),
                req.getCompletedAt()
        );
    }
}
