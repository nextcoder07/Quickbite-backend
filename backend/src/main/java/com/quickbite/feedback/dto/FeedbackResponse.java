package com.quickbite.feedback.dto;

import com.quickbite.feedback.Feedback;
import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Long orderId,
        Long buyerId,
        String buyerName,
        int rating,
        String comment,
        Instant createdAt
) {
    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getOrder().getId(),
                f.getBuyer().getId(),
                f.getBuyer().getDisplayName(),
                f.getRating(),
                f.getComment(),
                f.getCreatedAt()
        );
    }
}
