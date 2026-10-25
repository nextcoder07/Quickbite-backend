package com.quickbite.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateRefundRequest(
        @NotNull(message = "paymentId is required")
        Long paymentId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1.00", message = "Refund amount must be at least 1.00")
        BigDecimal amount,

        @NotBlank(message = "Reason is required")
        String reason,

        String idempotencyKey
) {}
