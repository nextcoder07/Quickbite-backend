package com.quickbite.administration;

import com.quickbite.accountdeletion.AccountDeletionService;
import com.quickbite.accountdeletion.dto.DeletionRequestResponse;
import com.quickbite.administration.dto.AdminAccountResponse;
import com.quickbite.administration.dto.AdminDashboardSummary;
import com.quickbite.administration.dto.AdminRoleRequestResponse;
import com.quickbite.administration.dto.AuditLogResponse;
import com.quickbite.administration.dto.DeliveryPartnerAdminResponse;
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
@Tag(name = "Administration & Moderation", description = "Administrative dashboard, shop moderation, admin permissions, deletion reviews, and audit logs")
public class AdminController {

    private final AdminService adminService;
    private final AccountDeletionService deletionService;

    public AdminController(AdminService adminService, AccountDeletionService deletionService) {
        this.adminService = adminService;
        this.deletionService = deletionService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard metrics", description = "Operational metrics on users, shops, orders, sales volume, and pending tasks")
    public ResponseEntity<AdminDashboardSummary> getSummary(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(adminService.getSummary(principal));
    }

    @GetMapping("/shops")
    @Operation(summary = "List all shops", description = "Retrieves all marketplace shops including pending, approved, and suspended")
    public ResponseEntity<List<ShopResponse>> getAllShops(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(adminService.getAllShops(principal));
    }

    @PatchMapping("/shops/{shopId}/status")
    @Operation(summary = "Review shop application", description = "Approve, reject, or suspend a marketplace shop")
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

    @PostMapping("/shops/{shopId}/deactivate")
    @Operation(summary = "Remove shop from platform", description = "Admin confirmation to permanently deactivate a shop")
    public ResponseEntity<ShopResponse> deactivateShop(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long shopId) {
        return ResponseEntity.ok(adminService.deactivateShop(principal, shopId));
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
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(adminService.getAuditLogs(principal));
    }

    @GetMapping("/delivery-partners")
    @Operation(summary = "List all delivery partners", description = "Retrieves all delivery partners with verification status and fulfillment stats")
    public ResponseEntity<List<DeliveryPartnerAdminResponse>> getAllDeliveryPartners(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(adminService.getAllDeliveryPartners(principal));
    }

    @PatchMapping("/delivery-partners/{partnerId}/status")
    @Operation(summary = "Verify or update delivery partner status", description = "Admin verifies/approves, rejects, suspends, or removes a delivery partner")
    public ResponseEntity<DeliveryPartnerAdminResponse> reviewDeliveryPartnerStatus(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long partnerId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        return ResponseEntity.ok(adminService.reviewDeliveryPartnerStatus(principal, partnerId, statusStr));
    }

    @GetMapping("/admins")
    @Operation(summary = "List all admins", description = "Retrieves active, pending, rejected, and suspended admin accounts")
    public ResponseEntity<List<AdminAccountResponse>> getAllAdmins(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(adminService.getAllAdmins(principal));
    }

    @PatchMapping("/admins/{adminId}/status")
    @Operation(summary = "Approve, reject, or suspend an admin", description = "Approves or rejects pending admin signups; suspending/reactivating an admin requires the admin security key")
    public ResponseEntity<AdminAccountResponse> reviewAdminStatus(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long adminId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String adminKey = body.get("adminKey");
        return ResponseEntity.ok(adminService.reviewAdminStatus(principal, adminId, statusStr, adminKey));
    }

    @GetMapping("/admin-requests")
    @Operation(summary = "List existing-user admin permission requests", description = "Retrieves requests from users asking Admins for permission to become an Admin")
    public ResponseEntity<List<AdminRoleRequestResponse>> getAdminRoleRequests(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(adminService.getAdminRoleRequests(principal));
    }

    @PostMapping("/admin-requests/{requestId}/review")
    @Operation(summary = "Approve or reject admin permission request", description = "Grants or declines Admin permissions for an existing platform user")
    public ResponseEntity<AdminRoleRequestResponse> reviewAdminRoleRequest(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String note = (String) body.get("note");
        return ResponseEntity.ok(adminService.reviewAdminRoleRequest(principal, requestId, approve, note));
    }
}
