package com.quickbite.identity.dto;

import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.common.enums.VerificationStatus;

public record AuthResponse(
        Long identityId,
        String firebaseUid,
        String email,
        AccountTypeSnapshot accountType,
        Long profileId,
        String displayName,
        String phone,
        AccountStatus status,
        VerificationStatus verificationStatus,
        boolean hasShop,
        Long shopId,
        ShopStatus shopApprovalStatus,
        boolean deletionPending,
        String defaultAddress,
        Double defaultLatitude,
        Double defaultLongitude,
        String foodInterests,
        String vehicleDetails,
        String serviceArea,
        String token
) {
    public AuthResponse(
            Long identityId,
            String firebaseUid,
            String email,
            AccountTypeSnapshot accountType,
            Long profileId,
            String displayName,
            String phone,
            AccountStatus status,
            String token
    ) {
        this(identityId, firebaseUid, email, accountType, profileId, displayName, phone, status,
                VerificationStatus.APPROVED, false, null, null, false, null, null, null, null, null, null, token);
    }
}
