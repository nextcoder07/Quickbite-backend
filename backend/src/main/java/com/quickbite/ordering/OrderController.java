package com.quickbite.ordering;

import com.quickbite.common.enums.OrderStatus;
import com.quickbite.ordering.dto.CheckoutRequest;
import com.quickbite.ordering.dto.OrderResponse;
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
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders & Checkout", description = "Checkout, order lifecycle management, and order tracking")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Place order", description = "Validates cart, reserves stock atomically, computes authoritative pricing, and creates the order")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(principal, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/buyer")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Buyer order history", description = "Retrieves all past and active orders for the authenticated buyer")
    public ResponseEntity<List<OrderResponse>> getBuyerOrders(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(orderService.getBuyerOrders(principal));
    }

    @GetMapping("/shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Shop incoming orders", description = "Retrieves orders placed at the shop owner's shop")
    public ResponseEntity<List<OrderResponse>> getShopOrders(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(orderService.getShopOrders(principal));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details", description = "Retrieves details and items for an order")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(principal, orderId));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Transitions order through lifecycle states (ACCEPTED, PREPARING, READY, etc.)")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status is required");
        }
        OrderStatus status = OrderStatus.valueOf(statusStr.toUpperCase());
        return ResponseEntity.ok(orderService.updateOrderStatus(principal, orderId, status));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an eligible order and restores product inventory")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Cancelled by user";
        return ResponseEntity.ok(orderService.cancelOrder(principal, orderId, reason));
    }
}
