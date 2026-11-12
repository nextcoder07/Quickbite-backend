package com.quickbite.payment.dto;

import com.quickbite.ordering.dto.CheckoutRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteCheckoutPaymentRequest(
        @NotNull(message = "checkout details are required")
        @Valid
        CheckoutRequest checkout,

        @NotBlank(message = "providerOrderId is required")
        String providerOrderId,

        @NotBlank(message = "providerPaymentId is required")
        String providerPaymentId,

        @NotBlank(message = "signature is required")
        String signature
) {}

