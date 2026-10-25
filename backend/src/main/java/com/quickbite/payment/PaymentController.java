package com.quickbite.payment;

import com.quickbite.payment.dto.*;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments & Refunds", description = "Endpoints for Razorpay test-mode payment initialization, verification, and refunds")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Create payment order", description = "Initializes Razorpay test-mode payment order on the server")
    public ResponseEntity<PaymentResponse> createPaymentOrder(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request) {
        return new ResponseEntity<>(paymentService.createPaymentOrder(principal, request), HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Verify payment signature", description = "Server-side cryptographic verification of Razorpay payment signature")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(principal, request));
    }

    @PostMapping("/refund/request")
    @Operation(summary = "Request refund", description = "Submits a refund request for a captured payment subject to balance caps")
    public ResponseEntity<RefundResponse> requestRefund(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreateRefundRequest request) {
        return new ResponseEntity<>(paymentService.requestRefund(principal, request), HttpStatus.CREATED);
    }

    @PostMapping("/refund/{refundId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Review refund", description = "Admin decision to approve or decline a refund")
    public ResponseEntity<RefundResponse> reviewRefund(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String note = (String) body.get("note");
        return ResponseEntity.ok(paymentService.reviewRefund(principal, refundId, approve, note));
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all refunds", description = "Admin access to all refund requests and statuses")
    public ResponseEntity<List<RefundResponse>> getRefunds() {
        return ResponseEntity.ok(paymentService.getRefunds());
    }
}
