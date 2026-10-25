package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "orderId is required")
        Long orderId,
        String idempotencyKey
) {}
