package com.quickbite.identity;

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
@Tag(name = "Authentication & Identity", description = "Endpoints for user registration, login, and profile lookup")
public class AuthController {

    private final AuthService authService;
    private final AccountIdentityRepository identityRepository;

    public AuthController(AuthService authService, AccountIdentityRepository identityRepository) {
        this.authService = authService;
        this.identityRepository = identityRepository;
    }

    @PostMapping("/signup")
    @Operation(summary = "Register profile", description = "Provisions a Buyer, Delivery Partner, or Shop Owner profile for an authenticated identity")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.registerProfile(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Retrieves the active profile and role for the current Bearer token")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        AuthResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dev-login")
    @Operation(summary = "Developer quick login", description = "Allows immediate role-based test authentication in local/development environment")
    public ResponseEntity<AuthResponse> devLogin(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "buyer@quickbite.campus").toLowerCase().trim();
        AccountIdentity identity = identityRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> identityRepository.save(new AccountIdentity("dev-uid-" + email, email)));
        AuthResponse response = authService.resolveAuthResponse(identity);
        return ResponseEntity.ok(response);
    }
}
