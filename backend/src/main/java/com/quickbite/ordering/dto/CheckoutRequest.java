package com.quickbite.ordering.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

        @JsonAlias("notes")
        String buyerNote,

        String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude
) {
    public CheckoutRequest(Long shopId, List<OrderItemRequest> items, String buyerNote) {
        this(shopId, items, buyerNote, null, null, null);
    }
}
