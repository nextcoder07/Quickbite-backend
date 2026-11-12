package com.quickbite.identity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes QuickBite account provisioning and role/verification status
 * (pending, verified, rejected, suspended, removed — excluding verification states for buyers)
 * with Firebase Authentication when Firebase Auth is enabled.
 */
@Service
public class FirebaseAccountSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAccountSyncService.class);

    public record ProvisionResult(String firebaseUid, boolean createdNew) {}

    private final boolean firebaseEnabled;
    private final Supplier<FirebaseAuth> firebaseAuthSupplier;

    @Autowired
    public FirebaseAccountSyncService(@Value("${quickbite.firebase.enabled:true}") boolean firebaseEnabled) {
        this(firebaseEnabled, () -> FirebaseApp.getApps().isEmpty() ? null : FirebaseAuth.getInstance());
    }

    FirebaseAccountSyncService(boolean firebaseEnabled, Supplier<FirebaseAuth> firebaseAuthSupplier) {
        this.firebaseEnabled = firebaseEnabled;
        this.firebaseAuthSupplier = firebaseAuthSupplier;
    }

    public boolean isActive() {
        return firebaseEnabled;
    }

    public ProvisionResult ensureFirebaseUserForSignup(
            String requestedUid,
            String normalizedEmail,
            String rawPassword,
            String displayName) {
        if (!firebaseEnabled) {
            String devUid = (requestedUid != null && !requestedUid.isBlank())
                    ? requestedUid.trim()
                    : "uid-" + UUID.randomUUID().toString().substring(0, 12);
            return new ProvisionResult(devUid, false);
        }

        FirebaseAuth auth = requireFirebaseAuth();

        if (requestedUid != null && !requestedUid.isBlank() && !requestedUid.trim().startsWith("uid-")) {
            String trimmedUid = requestedUid.trim();
            try {
                UserRecord existing = auth.getUser(trimmedUid);
                if (existing.getEmail() != null && !existing.getEmail().equalsIgnoreCase(normalizedEmail)) {
                    throw new ForbiddenException("Provided Firebase UID does not match signup email");
                }
                return new ProvisionResult(existing.getUid(), false);
            } catch (FirebaseAuthException ex) {
                throw new BadRequestException("Invalid or unknown Firebase UID: " + ex.getMessage());
            }
        }

        Optional<UserRecord> existingByEmail = findByEmail(auth, normalizedEmail);
        if (existingByEmail.isPresent()) {
            throw new ConflictException(
                    "An account with this email already exists in Firebase Authentication. Please sign in instead.");
        }

        try {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(normalizedEmail)
                    .setPassword(rawPassword)
                    .setDisplayName(displayName)
                    .setEmailVerified(false)
                    .setDisabled(false);
            UserRecord created = auth.createUser(createRequest);
            log.info("Created Firebase Auth user uid={} email={}", created.getUid(), normalizedEmail);
            return new ProvisionResult(created.getUid(), true);
        } catch (FirebaseAuthException ex) {
            if (ex.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new ConflictException(
                        "An account with this email already exists in Firebase Authentication. Please sign in instead.");
            }
            throw new BadRequestException("Could not create Firebase Auth account: " + ex.getMessage());
        }
    }

    public void syncAccountStatus(
            String firebaseUid,
            AccountTypeSnapshot role,
            VerificationStatus verificationStatus,
            AccountStatus accountStatus) {
        if (!firebaseEnabled) {
            return;
        }
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new BadRequestException("Missing Firebase UID for account status synchronization.");
        }

        FirebaseAuth auth = requireFirebaseAuth();
        AccountStatus effectiveAccountStatus = accountStatus != null ? accountStatus : AccountStatus.ACTIVE;
        Map<String, Object> claims = buildClaims(role, verificationStatus, effectiveAccountStatus);

        try {
            auth.setCustomUserClaims(firebaseUid, claims);
            boolean shouldDisable = effectiveAccountStatus == AccountStatus.DEACTIVATED;
            auth.updateUser(new UserRecord.UpdateRequest(firebaseUid).setDisabled(shouldDisable));
            log.info("Synced Firebase Auth user uid={} claims={} disabled={}", firebaseUid, claims, shouldDisable);
        } catch (FirebaseAuthException ex) {
            throw new BadRequestException("Failed to sync account status with Firebase Authentication: " + ex.getMessage());
        }
    }

    public void syncAccountStatusSafely(
            String firebaseUid,
            AccountTypeSnapshot role,
            VerificationStatus verificationStatus,
            AccountStatus accountStatus) {
        syncAccountStatus(firebaseUid, role, verificationStatus, accountStatus);
    }

    public void rollbackCreatedUser(ProvisionResult provisionResult) {
        if (!firebaseEnabled || provisionResult == null || !provisionResult.createdNew()) {
            return;
        }
        String uid = provisionResult.firebaseUid();
        if (uid == null || uid.isBlank()) {
            return;
        }
        try {
            FirebaseAuth auth = firebaseAuthSupplier.get();
            if (auth != null) {
                auth.deleteUser(uid);
                log.info("Rolled back newly created Firebase Auth user uid={}", uid);
            }
        } catch (Exception ex) {
            log.error("Failed to rollback Firebase Auth user uid={}: {}", uid, ex.getMessage());
        }
    }

    public static Map<String, Object> buildClaims(
            AccountTypeSnapshot role,
            VerificationStatus verificationStatus,
            AccountStatus accountStatus) {
        Map<String, Object> claims = new HashMap<>();
        AccountStatus effectiveAccountStatus = accountStatus != null ? accountStatus : AccountStatus.ACTIVE;
        if (role != null) {
            claims.put("role", role.name());
        }
        claims.put("accountStatus", effectiveAccountStatus.name());

        // Verification lifecycle (pending / verified / rejected / suspended) applies only to non-buyer accounts
        if (role != AccountTypeSnapshot.BUYER) {
            VerificationStatus effectiveVerification = verificationStatus != null
                    ? verificationStatus
                    : VerificationStatus.PENDING;
            String statusLabel = resolveNonBuyerStatusLabel(effectiveVerification, effectiveAccountStatus);
            claims.put("verificationStatus", effectiveVerification.name());
            claims.put("status", statusLabel);
            claims.put("verified", effectiveAccountStatus == AccountStatus.ACTIVE
                    && effectiveVerification == VerificationStatus.APPROVED);
        }

        return claims;
    }

    private static String resolveNonBuyerStatusLabel(
            VerificationStatus verificationStatus,
            AccountStatus accountStatus) {
        if (accountStatus == AccountStatus.DEACTIVATED) {
            return "removed";
        }
        if (accountStatus == AccountStatus.SUSPENDED || verificationStatus == VerificationStatus.SUSPENDED) {
            return "suspended";
        }
        return switch (verificationStatus) {
            case APPROVED -> "verified";
            case REJECTED -> "rejected";
            case PENDING -> "pending";
            case SUSPENDED -> "suspended";
        };
    }

    private Optional<UserRecord> findByEmail(FirebaseAuth auth, String email) {
        try {
            return Optional.of(auth.getUserByEmail(email));
        } catch (FirebaseAuthException ex) {
            if (ex.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                return Optional.empty();
            }
            throw new BadRequestException("Firebase Authentication lookup failed: " + ex.getMessage());
        }
    }

    private FirebaseAuth requireFirebaseAuth() {
        FirebaseAuth auth = firebaseAuthSupplier.get();
        if (auth == null) {
            throw new BadRequestException(
                    "Firebase Authentication is enabled but Firebase Admin SDK is not initialized.");
        }
        return auth;
    }
}

