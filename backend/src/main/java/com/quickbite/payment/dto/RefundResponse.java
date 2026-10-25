package com.quickbite.payment.dto;

import com.quickbite.common.enums.RefundStatus;
import com.quickbite.payment.Refund;
import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        Long id,
        Long paymentId,
        Long requestedByIdentityId,
        Long reviewedByAdminId,
        BigDecimal amount,
        RefundStatus status,
        String providerRefundId,
        String reason,
        Instant requestedAt,
        Instant processedAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getRequestedBy().getId(),
                refund.getReviewedBy() != null ? refund.getReviewedBy().getId() : null,
                refund.getAmount(),
                refund.getStatus(),
                refund.getProviderRefundId(),
                refund.getReason(),
                refund.getRequestedAt(),
                refund.getProcessedAt()
        );
    }
}
