package com.quickbite.shop.dto;

import java.math.BigDecimal;

public record UpdateProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String imagePath,
        Boolean available
) {}
