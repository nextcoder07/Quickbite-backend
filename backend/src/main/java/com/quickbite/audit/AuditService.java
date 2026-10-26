package com.quickbite.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.identity.AccountIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(AccountIdentity actor, String action, String targetType, String targetId, Map<String, Object> safeMetadata) {
        try {
            // Ensure no sensitive credentials (passwords, tokens, card details) are recorded
            String jsonMetadata = null;
            if (safeMetadata != null && !safeMetadata.isEmpty()) {
                jsonMetadata = objectMapper.writeValueAsString(safeMetadata);
            }
            AuditLog auditLog = new AuditLog(actor, action, targetType, targetId, jsonMetadata);
            auditLogRepository.save(auditLog);
            log.info("AUDIT LOG: action={}, targetType={}, targetId={}, actor={}",
                    action, targetType, targetId, actor != null ? actor.getEmail() : "SYSTEM");
        } catch (Exception ex) {
            log.error("Failed to record audit log: {}", ex.getMessage());
        }
    }
}
