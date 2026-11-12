package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteDeliveryRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Delivery OTP must be six digits")
        String otp
) {}
