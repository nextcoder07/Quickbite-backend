package com.quickbite.administration.dto;

import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.delivery.DeliveryPartner;
import java.time.Instant;

public record DeliveryPartnerAdminResponse(
        Long id,
        Long identityId,
        String email,
        String displayName,
        String phone,
        String vehicleDetails,
        String serviceArea,
        AccountStatus accountStatus,
        VerificationStatus verificationStatus,
        Instant createdAt,
        Instant updatedAt,
        long completedDeliveriesCount,
        long activeDeliveriesCount
) {
    public static DeliveryPartnerAdminResponse of(DeliveryPartner partner, long completed, long active) {
        return new DeliveryPartnerAdminResponse(
                partner.getId(),
                partner.getIdentity().getId(),
                partner.getIdentity().getEmail(),
                partner.getDisplayName(),
                partner.getPhone(),
                partner.getVehicleDetails(),
                partner.getServiceArea(),
                partner.getAccountStatus(),
                partner.getVerificationStatus(),
                partner.getCreatedAt(),
                partner.getUpdatedAt(),
                completed,
                active
        );
    }
}
