package com.quickbite.buyer.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "displayName is required")
        String displayName,
        String phone
) {}
