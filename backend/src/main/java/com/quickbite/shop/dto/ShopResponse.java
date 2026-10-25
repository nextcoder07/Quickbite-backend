package com.quickbite.shop.dto;

import com.quickbite.common.enums.ShopStatus;
import com.quickbite.shop.Shop;

public record ShopResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String imagePath,
        String campusLocation,
        ShopStatus approvalStatus,
        boolean open,
        boolean deactivated
) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getOwner().getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getImagePath(),
                shop.getCampusLocation(),
                shop.getApprovalStatus(),
                shop.isOpen(),
                shop.isDeactivated()
        );
    }
}
