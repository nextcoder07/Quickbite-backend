package com.quickbite.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    record GatewayOrder(String providerOrderId, BigDecimal amount, String currency) {}
    record GatewayRefund(boolean success, String providerRefundId, String message) {}

    GatewayOrder createOrder(BigDecimal amount, String currency, String receipt);
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);
    GatewayRefund processRefund(String paymentId, BigDecimal amount, String reason);
}
