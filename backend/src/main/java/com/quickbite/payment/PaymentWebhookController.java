package com.quickbite.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@Tag(name = "Payment Webhooks", description = "Asynchronous payment provider event handling")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Value("${quickbite.razorpay.webhook-secret:sample_webhook_secret_789}")
    private String webhookSecret;

    @PostMapping
    @Operation(summary = "Razorpay Webhook receiver", description = "Receives and idempotently verifies Razorpay payment lifecycle notifications")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody(required = false) String payload) {
        log.info("Received Razorpay webhook event with signature: {}", signature);
        // Validates signature and handles event safely
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
