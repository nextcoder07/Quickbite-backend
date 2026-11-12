package com.quickbite.delivery.dto;

import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.delivery.DeliveryAssignment;
import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryResponse(
        Long id,
        Long orderId,
        String orderNumber,
        String shopName,
        String shopLocation,
        Double shopLatitude,
        Double shopLongitude,
        String buyerName,
        String buyerPhone,
        String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude,
        String buyerNote,
        Long deliveryPartnerId,
        String deliveryPartnerName,
        DeliveryStatus status,
        Instant assignedAt,
        Instant acceptedAt,
        Instant pickedUpAt,
        Instant deliveredAt,
        String failureReason,
        BigDecimal orderTotal
) {
    public static DeliveryResponse from(DeliveryAssignment da) {
        return new DeliveryResponse(
                da.getId(),
                da.getOrder().getId(),
                da.getOrder().getOrderNumber(),
                da.getOrder().getShop().getName(),
                da.getOrder().getShop().getLocation(),
                da.getOrder().getShop().getLatitude(),
                da.getOrder().getShop().getLongitude(),
                da.getOrder().getBuyer().getDisplayName(),
                da.getOrder().getBuyer().getPhone(),
                da.getOrder().getDeliveryAddress(),
                da.getOrder().getDeliveryLatitude(),
                da.getOrder().getDeliveryLongitude(),
                da.getOrder().getBuyerNote(),
                da.getDeliveryPartner() != null ? da.getDeliveryPartner().getId() : null,
                da.getDeliveryPartner() != null ? da.getDeliveryPartner().getDisplayName() : null,
                da.getStatus(),
                da.getAssignedAt(),
                da.getAcceptedAt(),
                da.getPickedUpAt(),
                da.getDeliveredAt(),
                da.getFailureReason(),
                da.getOrder().getTotalAmount()
        );
    }
}
