package com.quickbite.accountdeletion;

import com.quickbite.accountdeletion.dto.CreateDeletionRequest;
import com.quickbite.accountdeletion.dto.DeletionRequestResponse;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account/deletion-request")
@Tag(name = "Account Deletion Requests", description = "Endpoints for Shop Owners and Delivery Partners to request account deletion")
public class AccountDeletionController {

    private final AccountDeletionService deletionService;

    public AccountDeletionController(AccountDeletionService deletionService) {
        this.deletionService = deletionService;
    }

    @PostMapping
    @Operation(summary = "Submit deletion request", description = "Shop Owners and Delivery Partners submit account deletion requests for admin review")
    public ResponseEntity<DeletionRequestResponse> submitRequest(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreateDeletionRequest request) {
        return new ResponseEntity<>(deletionService.submitRequest(principal, request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get my deletion requests", description = "Retrieves the status of previously submitted deletion requests")
    public ResponseEntity<List<DeletionRequestResponse>> getMyRequests(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(deletionService.getMyRequests(principal));
    }
}
