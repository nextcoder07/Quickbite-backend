package com.quickbite.administration;

import com.quickbite.common.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validates operations protected by the environment-configured Admin Security Key.
 */
@Component
public class AdminKeyValidator {

    private final String configuredKey;

    public AdminKeyValidator(@Value("${quickbite.admin.signup-key:}") String configuredKey) {
        this.configuredKey = configuredKey != null ? configuredKey.trim() : "";
    }

    public boolean isValid(String candidateKey) {
        if (configuredKey.isBlank() || candidateKey == null || candidateKey.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                candidateKey.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    public void requireValid(String candidateKey, String errorMessage) {
        if (!isValid(candidateKey)) {
            throw new ForbiddenException(errorMessage);
        }
    }
}

