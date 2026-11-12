package com.quickbite.identity.dto;

import com.quickbite.common.enums.AccountTypeSnapshot;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        String firebaseUid,

        @NotBlank(message = "email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "displayName is required")
        String displayName,

        String phone,

        @NotNull(message = "accountType is required")
        AccountTypeSnapshot accountType,

        String password,
        String vehicleDetails,
        String serviceArea,
        String defaultAddress,
        Double defaultLatitude,
        Double defaultLongitude,
        String foodInterests,
        String adminKey,
        String adminRequestNote
) {
    public SignupRequest(String firebaseUid, String email, String displayName, String phone, AccountTypeSnapshot accountType) {
        this(firebaseUid, email, displayName, phone, accountType, null, null, null, null, null, null, null, null, null);
    }

    public SignupRequest(String firebaseUid, String email, String displayName, String phone, AccountTypeSnapshot accountType,
                         String password, String vehicleDetails, String serviceArea,
                         String defaultAddress, Double defaultLatitude, Double defaultLongitude, String foodInterests) {
        this(firebaseUid, email, displayName, phone, accountType, password, vehicleDetails, serviceArea,
                defaultAddress, defaultLatitude, defaultLongitude, foodInterests, null, null);
    }
}
