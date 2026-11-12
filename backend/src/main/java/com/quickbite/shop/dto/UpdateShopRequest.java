package com.quickbite.shop.dto;

public record UpdateShopRequest(
        String name,
        String description,
        String location,
        String campusLocation,
        String imagePath,
        Boolean open,
        Double latitude,
        Double longitude
) {
    public String resolvedLocation() {
        if (location != null && !location.isBlank()) {
            return location.trim();
        }
        return campusLocation != null && !campusLocation.isBlank() ? campusLocation.trim() : null;
    }
}
