package com.quickbite.identity.dto;

import com.quickbite.common.enums.AccountTypeSnapshot;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        @NotBlank(message = "firebaseUid is required")
        String firebaseUid,

        @NotBlank(message = "email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "displayName is required")
        String displayName,

        String phone,

        @NotNull(message = "accountType is required")
        AccountTypeSnapshot accountType
) {}
