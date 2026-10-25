package com.quickbite.shop.dto;

public record UpdateShopRequest(
        String name,
        String description,
        String campusLocation,
        String imagePath,
        Boolean open
) {}
