package com.quickbite.ordering.dto;

import com.quickbite.common.enums.OrderStatus;
import com.quickbite.ordering.CustomerOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long shopId,
        String shopName,
        String shopLocation,
        Long buyerId,
        String buyerName,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String buyerNote,
        String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude,
        String cancellationReason,
        Instant placedAt,
        List<OrderItemDto> items
) {
    public static OrderResponse from(CustomerOrder order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(OrderItemDto::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getShop().getId(),
                order.getShop().getName(),
                order.getShop().getLocation(),
                order.getBuyer().getId(),
                order.getBuyer().getDisplayName(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getBuyerNote(),
                order.getDeliveryAddress(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude(),
                order.getCancellationReason(),
                order.getPlacedAt(),
                itemDtos
        );
    }
}
