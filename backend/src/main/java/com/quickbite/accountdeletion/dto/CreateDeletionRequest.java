package com.quickbite.accountdeletion.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeletionRequest(
        @NotBlank(message = "Reason for deletion request is required")
        String reason
) {}
