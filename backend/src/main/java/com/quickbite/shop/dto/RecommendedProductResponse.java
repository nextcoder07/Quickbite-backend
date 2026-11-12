package com.quickbite.shop.dto;

import java.math.BigDecimal;

/**
 * Projection representing a recommended food item ranked by top ratings,
 * buyer profile interests, past purchases, or the fresh-user cold-start algorithm.
 */
public record RecommendedProductResponse(
        Long productId,
        String productName,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imagePath,
        Long shopId,
        String shopName,
        String shopLocation,
        Double shopLatitude,
        Double shopLongitude,
        boolean shopOpen,
        double averageRating,
        int ratingCount,
        int popularityCount,
        double recommendationScore,
        String recommendationReason,
        boolean coldStartPick
) {
}

