package com.quickbite.shop.dto;

import com.quickbite.shop.Product;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        Long shopId,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imagePath,
        boolean available
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getShop().getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getImagePath(),
                p.isAvailable()
        );
    }
}
