package com.quickbite.delivery;

import com.quickbite.delivery.dto.DeliveryResponse;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery")
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
@Tag(name = "Delivery Operations", description = "Delivery fulfillment for assigned delivery partners")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/available")
    @Operation(summary = "Available deliveries", description = "List unassigned orders ready for delivery")
    public ResponseEntity<List<DeliveryResponse>> getAvailableDeliveries() {
        return ResponseEntity.ok(deliveryService.getAvailableDeliveries());
    }

    @GetMapping("/my")
    @Operation(summary = "My deliveries", description = "List orders assigned to the authenticated partner")
    public ResponseEntity<List<DeliveryResponse>> getMyDeliveries(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(principal));
    }

    @PostMapping("/{assignmentId}/claim")
    @Operation(summary = "Claim delivery", description = "Assigns an available delivery to current partner")
    public ResponseEntity<DeliveryResponse> claimAssignment(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(deliveryService.claimAssignment(principal, assignmentId));
    }

    @PostMapping("/{assignmentId}/pickup")
    @Operation(summary = "Mark order picked up", description = "Transitions status to PICKED_UP and order to OUT_FOR_DELIVERY")
    public ResponseEntity<DeliveryResponse> markPickedUp(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(deliveryService.markPickedUp(principal, assignmentId));
    }

    @PostMapping("/{assignmentId}/deliver")
    @Operation(summary = "Mark order delivered", description = "Transitions status to DELIVERED and marks customer order DELIVERED")
    public ResponseEntity<DeliveryResponse> markDelivered(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(deliveryService.markDelivered(principal, assignmentId));
    }

    @PostMapping("/{assignmentId}/fail")
    @Operation(summary = "Report delivery failure", description = "Records delivery failure with an explanation reason")
    public ResponseEntity<DeliveryResponse> reportFailure(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long assignmentId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Delivery failure";
        return ResponseEntity.ok(deliveryService.reportFailure(principal, assignmentId, reason));
    }
}
