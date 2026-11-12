package com.quickbite.payment.dto;

import java.math.BigDecimal;

public record InitiateCheckoutPaymentResponse(
        String providerOrderId,
        BigDecimal amount,
        String currency,
        String razorpayKeyId,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {}

