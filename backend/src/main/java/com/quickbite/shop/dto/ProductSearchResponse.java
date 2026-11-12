package com.quickbite.shop.dto;

import com.quickbite.shop.Product;
import com.quickbite.shop.Shop;

import java.math.BigDecimal;

/**
 * Search projection allowing buyers to discover individual food items across any shop and place.
 */
public record ProductSearchResponse(
        Long productId,
        String productName,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imagePath,
        Long shopId,
        String shopName,
        String shopLocation,
        boolean shopOpen
) {
    public static ProductSearchResponse from(Product product) {
        Shop shop = product.getShop();
        return new ProductSearchResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImagePath(),
                shop.getId(),
                shop.getName(),
                shop.getLocation(),
                shop.isOpen()
        );
    }
}

