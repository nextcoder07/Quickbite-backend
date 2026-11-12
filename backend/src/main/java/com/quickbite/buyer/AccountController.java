package com.quickbite.buyer;

import com.quickbite.administration.dto.AdminRoleRequestResponse;
import com.quickbite.buyer.dto.UpdateProfileRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account Management", description = "Endpoints for profile modification and self-deactivation")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update buyer profile", description = "Updates the display name and phone number of the authenticated buyer")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = accountService.updateProfile(principal, request);
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("message", "Profile updated successfully");
        payload.put("displayName", user.getDisplayName());
        payload.put("phone", user.getPhone() != null ? user.getPhone() : "");
        payload.put("defaultAddress", user.getDefaultAddress() != null ? user.getDefaultAddress() : "");
        payload.put("defaultLatitude", user.getDefaultLatitude());
        payload.put("defaultLongitude", user.getDefaultLongitude());
        payload.put("foodInterests", user.getFoodInterests() != null ? user.getFoodInterests() : "");
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate account", description = "Allows a buyer to self-deactivate their account while preserving order history")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        accountService.deactivateAccount(principal);
        return ResponseEntity.ok(Map.of("message", "Account successfully deactivated"));
    }

    @PostMapping("/admin-request")
    @Operation(summary = "Request admin role permission", description = "Allows an authenticated user or pending admin to ask existing Admins for Admin permission")
    public ResponseEntity<AdminRoleRequestResponse> requestAdminRole(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(accountService.requestAdminRole(principal, reason));
    }

    @GetMapping("/admin-request")
    @Operation(summary = "List my admin role requests", description = "Retrieves Admin role promotion requests submitted by the current user")
    public ResponseEntity<List<AdminRoleRequestResponse>> getMyAdminRequests(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(accountService.getMyAdminRequests(principal));
    }
}
