package com.quickbite.identity;

import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Identity", description = "Endpoints for Firebase-authenticated user registration, login, logout, and profile lookup")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Provision QuickBite profile for a Firebase-authenticated user")
    public ResponseEntity<AuthResponse> signup(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody SignupRequest request) {
        if (principal == null || principal.getFirebaseUid() == null || principal.getFirebaseUid().isBlank()) {
            throw new ForbiddenException("Firebase ID token is required to create an account");
        }
        return new ResponseEntity<>(authService.registerFirebasePrincipal(principal, request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Resolve QuickBite Supabase profile for an authenticated Firebase session")
    public ResponseEntity<AuthResponse> login(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        if (principal == null || principal.getFirebaseUid() == null || principal.getFirebaseUid().isBlank()) {
            throw new ForbiddenException("Firebase ID token is required to sign in");
        }
        return ResponseEntity.ok(authService.loginWithFirebasePrincipal(principal));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out", description = "Audits account logout for the active Firebase bearer session")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Retrieves the active Supabase profile, role, and verification state for the current Firebase Bearer token")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        AuthResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(response);
    }
}
