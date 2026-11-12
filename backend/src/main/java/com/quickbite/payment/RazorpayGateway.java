package com.quickbite.payment;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final String keyId;
    private final String keySecret;
    private final boolean mockEnabled;
    private RazorpayClient client;

    public RazorpayGateway(String keyId, String keySecret) {
        this(keyId, keySecret, true);
    }

    @Autowired
    public RazorpayGateway(
            @Value("${quickbite.razorpay.key-id:}") String keyId,
            @Value("${quickbite.razorpay.key-secret:}") String keySecret,
            @Value("${quickbite.razorpay.mock-enabled:true}") boolean mockEnabled) {
        this.keyId = keyId != null ? keyId.trim() : "";
        this.keySecret = keySecret != null ? keySecret.trim() : "";
        this.mockEnabled = mockEnabled;
        if (!this.mockEnabled) {
            if (this.keyId.isEmpty() || this.keySecret.isEmpty()) {
                throw new IllegalStateException(
                        "Razorpay payment gateway is not configured (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET missing).");
            }
            try {
                this.client = new RazorpayClient(this.keyId, this.keySecret);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize Razorpay client: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) {
        if (mockEnabled) {
            String testOrderId = "order_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            return new GatewayOrder(testOrderId, amount, currency != null ? currency : "INR");
        }
        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // in paise
            options.put("currency", currency != null ? currency : "INR");
            options.put("receipt", receipt);
            com.razorpay.Order order = client.orders.create(options);
            return new GatewayOrder(order.get("id"), amount, currency);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        if (mockEnabled && ("test_signature".equalsIgnoreCase(signature) || signature.startsWith("sig_test_"))) {
            return true;
        }
        if (keySecret.isEmpty()) {
            return false;
        }

        try {
            String data = orderId + "|" + paymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Signature calculation error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GatewayRefund processRefund(String paymentId, BigDecimal amount, String reason) {
        if (mockEnabled || paymentId == null || paymentId.startsWith("pay_test_")) {
            String testRefundId = "rfnd_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            return new GatewayRefund(true, testRefundId, "Test refund processed");
        }
        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            refundRequest.put("notes", new JSONObject().put("reason", reason));
            com.razorpay.Refund refund = client.payments.refund(paymentId, refundRequest);
            return new GatewayRefund(true, refund.get("id"), "Refund processed successfully");
        } catch (Exception e) {
            log.warn("Razorpay live refund failed: {}", e.getMessage());
            return new GatewayRefund(false, null, e.getMessage());
        }
    }

    public String getKeyId() {
        return keyId;
    }
}
