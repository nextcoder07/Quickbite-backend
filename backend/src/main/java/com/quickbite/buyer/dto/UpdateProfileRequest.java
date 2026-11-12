package com.quickbite.buyer.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "displayName is required")
        String displayName,
        String phone,
        String defaultAddress,
        Double defaultLatitude,
        Double defaultLongitude,
        String foodInterests
) {
    public UpdateProfileRequest(String displayName, String phone) {
        this(displayName, phone, null, null, null, null);
    }
}
