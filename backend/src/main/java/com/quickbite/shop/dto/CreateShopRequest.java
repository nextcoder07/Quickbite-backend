package com.quickbite.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShopRequest(
        @NotBlank(message = "Shop name is required")
        String name,
        String description,
        @NotBlank(message = "Campus location is required")
        String campusLocation,
        String imagePath
) {}
