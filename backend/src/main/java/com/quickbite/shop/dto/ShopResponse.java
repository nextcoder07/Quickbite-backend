package com.quickbite.shop.dto;

import com.quickbite.common.enums.ShopStatus;
import com.quickbite.shop.Shop;

import java.util.List;

public record ShopResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String imagePath,
        String location,
        String campusLocation,
        Double latitude,
        Double longitude,
        ShopStatus approvalStatus,
        boolean open,
        boolean deactivated,
        List<String> productNames
) {
    public static ShopResponse from(Shop shop, List<String> productNames) {
        return new ShopResponse(
                shop.getId(),
                shop.getOwner().getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getImagePath(),
                shop.getLocation(),
                shop.getCampusLocation(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getApprovalStatus(),
                shop.isOpen(),
                shop.isDeactivated(),
                productNames != null ? productNames : List.of()
        );
    }

    public static ShopResponse from(Shop shop) {
        return from(shop, List.of());
    }
}
