package com.quickbite.buyer;

import com.quickbite.buyer.dto.UpdateProfileRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully",
                "displayName", user.getDisplayName(),
                "phone", user.getPhone() != null ? user.getPhone() : ""
        ));
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate account", description = "Allows a buyer to self-deactivate their account while preserving order history")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        accountService.deactivateAccount(principal);
        return ResponseEntity.ok(Map.of("message", "Account successfully deactivated"));
    }
}
