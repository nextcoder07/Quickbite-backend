package com.quickbite.administration.dto;

import com.quickbite.audit.AuditLog;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String targetType,
        String targetId,
        String safeMetadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() != null ? log.getActor().getEmail() : "SYSTEM",
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSafeMetadata(),
                log.getCreatedAt()
        );
    }
}
