package com.quickbite.administration;

import com.quickbite.accountdeletion.AccountDeletionService;
import com.quickbite.accountdeletion.dto.DeletionRequestResponse;
import com.quickbite.administration.dto.AdminDashboardSummary;
import com.quickbite.administration.dto.AuditLogResponse;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.dto.ShopResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration & Moderation", description = "Administrative dashboard, shop moderation, deletion reviews, and audit logs")
public class AdminController {

    private final AdminService adminService;
    private final AccountDeletionService deletionService;

    public AdminController(AdminService adminService, AccountDeletionService deletionService) {
        this.adminService = adminService;
        this.deletionService = deletionService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard metrics", description = "Operational metrics on users, shops, orders, sales volume, and pending tasks")
    public ResponseEntity<AdminDashboardSummary> getSummary() {
        return ResponseEntity.ok(adminService.getSummary());
    }

    @GetMapping("/shops")
    @Operation(summary = "List all shops", description = "Retrieves all campus shops including pending, approved, and suspended")
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        return ResponseEntity.ok(adminService.getAllShops());
    }

    @PatchMapping("/shops/{shopId}/status")
    @Operation(summary = "Review shop application", description = "Approve, reject, or suspend a campus shop")
    public ResponseEntity<ShopResponse> reviewShop(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long shopId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status is required");
        }
        ShopStatus status = ShopStatus.valueOf(statusStr.toUpperCase());
        return ResponseEntity.ok(adminService.reviewShop(principal, shopId, status));
    }

    @GetMapping("/deletion-requests")
    @Operation(summary = "List deletion requests", description = "Retrieves pending and reviewed deletion requests")
    public ResponseEntity<List<DeletionRequestResponse>> getAllDeletionRequests() {
        return ResponseEntity.ok(deletionService.getAllRequests());
    }

    @PostMapping("/deletion-requests/{requestId}/review")
    @Operation(summary = "Review deletion request", description = "Admin confirms or declines account deletion request")
    public ResponseEntity<DeletionRequestResponse> reviewDeletionRequest(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> body) {
        boolean confirm = Boolean.TRUE.equals(body.get("confirm"));
        String note = (String) body.get("note");
        return ResponseEntity.ok(deletionService.reviewRequest(principal, requestId, confirm, note));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Inspect audit logs", description = "Retrieves append-only audit trail with safe metadata")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }
}
