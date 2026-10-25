package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyPaymentRequest(
        @NotNull(message = "orderId is required")
        Long orderId,

        @NotBlank(message = "providerOrderId is required")
        String providerOrderId,

        @NotBlank(message = "providerPaymentId is required")
        String providerPaymentId,

        @NotBlank(message = "signature is required")
        String signature
) {}
