package com.quickbite.dispute.dto;

import com.quickbite.common.enums.DisputeStatus;
import com.quickbite.dispute.Dispute;
import java.time.Instant;

public record DisputeResponse(
        Long id,
        Long orderId,
        String orderNumber,
        Long raisedByIdentityId,
        String category,
        String description,
        DisputeStatus status,
        String resolutionNote,
        Long resolvedByAdminId,
        Instant createdAt,
        Instant resolvedAt
) {
    public static DisputeResponse from(Dispute d) {
        return new DisputeResponse(
                d.getId(),
                d.getOrder().getId(),
                d.getOrder().getOrderNumber(),
                d.getRaisedBy().getId(),
                d.getCategory(),
                d.getDescription(),
                d.getStatus(),
                d.getResolutionNote(),
                d.getResolvedBy() != null ? d.getResolvedBy().getId() : null,
                d.getCreatedAt(),
                d.getResolvedAt()
        );
    }
}
