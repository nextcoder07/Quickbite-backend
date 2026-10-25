package com.quickbite.dispute;

import com.quickbite.dispute.dto.CreateDisputeRequest;
import com.quickbite.dispute.dto.DisputeResponse;
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
@RequestMapping("/api/v1/disputes")
@Tag(name = "Dispute Resolution", description = "Order dispute raising and administrative resolution")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Raise dispute", description = "Allows a buyer to lodge a formal dispute for their order")
    public ResponseEntity<DisputeResponse> raiseDispute(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreateDisputeRequest request) {
        return new ResponseEntity<>(disputeService.raiseDispute(principal, request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @Operation(summary = "My disputes", description = "Retrieves disputes raised by the current user")
    public ResponseEntity<List<DisputeResponse>> getMyDisputes(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(disputeService.getMyDisputes(principal));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all disputes", description = "Admin view of all open and resolved disputes")
    public ResponseEntity<List<DisputeResponse>> getAllDisputes() {
        return ResponseEntity.ok(disputeService.getAllDisputes());
    }

    @PostMapping("/{disputeId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve dispute", description = "Admin decision to resolve or reject an order dispute")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long disputeId,
            @RequestBody Map<String, Object> body) {
        boolean resolve = Boolean.TRUE.equals(body.get("resolve"));
        String note = (String) body.get("note");
        return ResponseEntity.ok(disputeService.resolveDispute(principal, disputeId, resolve, note));
    }
}
