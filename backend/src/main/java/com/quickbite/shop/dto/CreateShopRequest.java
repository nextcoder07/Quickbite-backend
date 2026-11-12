package com.quickbite.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShopRequest(
        @NotBlank(message = "Shop name is required")
        String name,
        String description,
        String location,
        String campusLocation,
        String imagePath,
        Double latitude,
        Double longitude
) {
    public CreateShopRequest(String name, String description, String campusLocation, String imagePath) {
        this(name, description, campusLocation, campusLocation, imagePath, null, null);
    }

    public String resolvedLocation() {
        if (location != null && !location.isBlank()) {
            return location.trim();
        }
        return campusLocation != null ? campusLocation.trim() : null;
    }
}
