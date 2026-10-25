package com.quickbite.ordering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CheckoutRequest(
        @NotNull(message = "shopId is required")
        Long shopId,

        @NotEmpty(message = "Cart cannot be empty")
        @Valid
        List<OrderItemRequest> items,

        String buyerNote
) {}
