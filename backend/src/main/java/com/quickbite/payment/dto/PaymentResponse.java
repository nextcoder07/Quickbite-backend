package com.quickbite.payment.dto;

import com.quickbite.common.enums.PaymentStatus;
import com.quickbite.payment.Payment;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        String provider,
        String providerOrderId,
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant verifiedAt,
        String razorpayKeyId
) {
    public static PaymentResponse from(Payment payment, String razorpayKeyId) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getProvider(),
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getVerifiedAt(),
                razorpayKeyId
        );
    }
}
