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
        String buyerName,
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
                da.getOrder().getShop().getCampusLocation(),
                da.getOrder().getBuyer().getDisplayName(),
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
