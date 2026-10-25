package com.quickbite.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDisputeRequest(
        @NotNull(message = "orderId is required")
        Long orderId,

        @NotBlank(message = "category is required")
        String category,

        @NotBlank(message = "description is required")
        String description
) {}
