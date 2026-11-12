package com.quickbite.identity;

import com.quickbite.accountdeletion.AccountDeletionRequestRepository;
import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminKeyValidator;
import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.DeletionRequestStatus;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
import com.quickbite.shop.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final AccountIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final AdminRepository adminRepository;
    private final AuditService auditService;
    private final ShopRepository shopRepository;
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final AdminKeyValidator adminKeyValidator;
    private final FirebaseAccountSyncService firebaseSyncService;

    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService) {
        this(identityRepository, userRepository, deliveryPartnerRepository, shopOwnerRepository,
                adminRepository, auditService, null, null,
                new AdminKeyValidator(""), new FirebaseAccountSyncService(false));
    }

    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService,
            AdminKeyValidator adminKeyValidator) {
        this(identityRepository, userRepository, deliveryPartnerRepository, shopOwnerRepository,
                adminRepository, auditService, null, null,
                adminKeyValidator, new FirebaseAccountSyncService(false));
    }

    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService,
            AdminKeyValidator adminKeyValidator,
            FirebaseAccountSyncService firebaseSyncService) {
        this(identityRepository, userRepository, deliveryPartnerRepository, shopOwnerRepository,
                adminRepository, auditService, null, null,
                adminKeyValidator, firebaseSyncService);
    }

    @Autowired
    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService,
            ShopRepository shopRepository,
            AccountDeletionRequestRepository deletionRequestRepository,
            AdminKeyValidator adminKeyValidator,
            FirebaseAccountSyncService firebaseSyncService) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
        this.shopRepository = shopRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.adminKeyValidator = adminKeyValidator != null ? adminKeyValidator : new AdminKeyValidator("");
        this.firebaseSyncService = firebaseSyncService != null ? firebaseSyncService : new FirebaseAccountSyncService(false);
    }

    public boolean isFirebaseAuthEnabled() {
        return firebaseSyncService.isActive();
    }

    @Transactional
    public AuthResponse registerFirebasePrincipal(CurrentAccountPrincipal principal, SignupRequest request) {
        if (principal == null || principal.getFirebaseUid() == null || principal.getFirebaseUid().isBlank()) {
            throw new ForbiddenException("Firebase authentication token is required to register an account");
        }
        String verifiedEmail = principal.getEmail() != null ? principal.getEmail().toLowerCase().trim() : null;
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new BadRequestException("Authenticated Firebase token does not contain an email address");
        }
        if (request.email() != null && !request.email().isBlank()
                && !request.email().trim().equalsIgnoreCase(verifiedEmail)) {
            throw new ForbiddenException("Signup email must match the authenticated Firebase user email");
        }
        if (request.accountType() == null) {
            throw new BadRequestException("Account profile type is required");
        }

        AdminSignupDecision adminDecision = evaluateAdminSignup(request);
        String displayName = request.displayName().trim();
        String phone = (request.phone() != null && !request.phone().isBlank()) ? request.phone().trim() : null;

        AccountIdentity identity = resolveOrCreateIdentityForSignup(principal.getFirebaseUid(), verifiedEmail);
        return persistRoleProfileAndSync(identity, displayName, phone, request, adminDecision);
    }

    private record AdminSignupDecision(VerificationStatus initialVerification) {}

    private AdminSignupDecision evaluateAdminSignup(SignupRequest request) {
        boolean hasAdminKey = request.adminKey() != null && !request.adminKey().isBlank();
        VerificationStatus adminInitialVerification = VerificationStatus.PENDING;
        if (request.accountType() == AccountTypeSnapshot.ADMIN && hasAdminKey) {
            adminKeyValidator.requireValid(request.adminKey(), "Invalid admin security key provided");
            adminInitialVerification = VerificationStatus.APPROVED;
        }
        return new AdminSignupDecision(adminInitialVerification);
    }

    private AccountIdentity resolveOrCreateIdentityForSignup(String firebaseUid, String normalizedEmail) {
        AccountIdentity existing = checkExistingIdentityConflicts(firebaseUid, normalizedEmail);
        if (existing != null) {
            return existing;
        }
        return identityRepository.saveAndFlush(new AccountIdentity(firebaseUid, normalizedEmail));
    }

    private AccountIdentity checkExistingIdentityConflicts(String firebaseUid, String normalizedEmail) {
        Optional<AccountIdentity> existingByUid = firebaseUid != null
                ? identityRepository.findByFirebaseUid(firebaseUid)
                : Optional.empty();
        Optional<AccountIdentity> existingByEmail = identityRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingByUid.isPresent() && existingByEmail.isPresent()
                && !existingByUid.get().getId().equals(existingByEmail.get().getId())) {
            throw new ConflictException("Provided Firebase UID and email belong to different accounts");
        }

        AccountIdentity existingIdentity = existingByUid.or(() -> existingByEmail).orElse(null);
        if (existingIdentity != null) {
            if (existingIdentity.getId() != null) {
                assertNoExistingProfile(existingIdentity.getId());
            }
            if (firebaseUid != null && !firebaseUid.isBlank() && !firebaseUid.equals(existingIdentity.getFirebaseUid())) {
                existingIdentity.updateFirebaseUid(firebaseUid);
                existingIdentity = identityRepository.saveAndFlush(existingIdentity);
            }
        }
        return existingIdentity;
    }

    private AuthResponse persistRoleProfileAndSync(
            AccountIdentity identity,
            String displayName,
            String phone,
            SignupRequest request,
            AdminSignupDecision adminDecision) {
        Long profileId;
        VerificationStatus initialVerification;
        switch (request.accountType()) {
            case BUYER -> {
                User newBuyer = new User(identity, displayName, phone);
                if (request.defaultAddress() != null || request.defaultLatitude() != null || request.defaultLongitude() != null) {
                    newBuyer.updateDeliveryLocation(request.defaultAddress(), request.defaultLatitude(), request.defaultLongitude());
                }
                if (request.foodInterests() != null && !request.foodInterests().isBlank()) {
                    newBuyer.updateFoodInterests(request.foodInterests());
                }
                User buyer = userRepository.saveAndFlush(newBuyer);
                profileId = buyer.getId();
                initialVerification = VerificationStatus.APPROVED;
            }
            case DELIVERY_PARTNER -> {
                String vehicle = request.vehicleDetails() != null && !request.vehicleDetails().isBlank()
                        ? request.vehicleDetails().trim()
                        : null;
                String area = request.serviceArea() != null && !request.serviceArea().isBlank()
                        ? request.serviceArea().trim()
                        : null;
                DeliveryPartner dp = new DeliveryPartner(identity, displayName, phone, vehicle, area);
                dp = deliveryPartnerRepository.saveAndFlush(dp);
                profileId = dp.getId();
                initialVerification = dp.getVerificationStatus();
            }
            case SHOP_OWNER -> {
                ShopOwner so = shopOwnerRepository.saveAndFlush(new ShopOwner(identity, displayName, phone));
                profileId = so.getId();
                initialVerification = VerificationStatus.PENDING;
            }
            case ADMIN -> {
                Admin admin = adminRepository.saveAndFlush(new Admin(
                        identity,
                        displayName,
                        adminDecision.initialVerification(),
                        request.adminRequestNote()
                ));
                profileId = admin.getId();
                initialVerification = admin.getVerificationStatus();
            }
            default -> throw new ForbiddenException("Unsupported account profile type");
        }

        firebaseSyncService.syncAccountStatus(
                identity.getFirebaseUid(),
                request.accountType(),
                initialVerification,
                AccountStatus.ACTIVE
        );

        String auditAction = request.accountType() == AccountTypeSnapshot.ADMIN
                ? (adminDecision.initialVerification() == VerificationStatus.APPROVED ? "ADMIN_CREATED_WITH_KEY" : "ADMIN_ACCESS_REQUESTED")
                : "PROFILE_CREATED";

        auditService.record(identity, auditAction, request.accountType().name(), String.valueOf(profileId),
                Map.of("email", identity.getEmail(), "role", request.accountType().name()));

        return resolveAuthResponse(identity);
    }

    @Transactional
    public AuthResponse loginWithFirebasePrincipal(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getFirebaseUid() == null || principal.getFirebaseUid().isBlank()) {
            throw new ForbiddenException("Authentication must be performed via Firebase");
        }

        Optional<AccountIdentity> identityOpt = identityRepository.findByFirebaseUid(principal.getFirebaseUid());
        if (identityOpt.isEmpty() && principal.getEmail() != null && !principal.getEmail().isBlank()) {
            identityOpt = identityRepository.findByEmailIgnoreCase(principal.getEmail().toLowerCase().trim());
        }

        AccountIdentity identity = identityOpt.orElseThrow(() -> new ResourceNotFoundException(
                "No QuickBite profile found in Supabase for this Firebase account. Please sign up first."));

        if (!principal.getFirebaseUid().equals(identity.getFirebaseUid())) {
            identity.updateFirebaseUid(principal.getFirebaseUid());
            identity = identityRepository.saveAndFlush(identity);
        }

        AuthResponse response = resolveAuthResponse(identity);
        if (response.status() == AccountStatus.DEACTIVATED) {
            throw new ForbiddenException("This account has been deactivated and removed from the platform");
        }

        firebaseSyncService.syncAccountStatus(
                identity.getFirebaseUid(),
                response.accountType(),
                response.verificationStatus(),
                response.status()
        );

        auditService.record(identity, "USER_LOGIN", response.accountType() != null ? response.accountType().name() : "IDENTITY",
                String.valueOf(response.profileId()), Map.of("email", identity.getEmail()));

        return response;
    }

    @Transactional
    public void logout(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            return;
        }
        identityRepository.findById(principal.getIdentityId()).ifPresent(identity ->
                auditService.record(identity, "USER_LOGOUT",
                        principal.getAccountType() != null ? principal.getAccountType().name() : "IDENTITY",
                        String.valueOf(principal.getProfileId()),
                        Map.of("email", identity.getEmail()))
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ResourceNotFoundException("Authenticated identity not found");
        }

        AccountIdentity identity = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Account identity not found"));

        AuthResponse response = resolveAuthResponse(identity);
        if (response.status() == AccountStatus.DEACTIVATED) {
            throw new ForbiddenException("Account has been removed from the platform");
        }
        return response;
    }

    @Transactional
    public AuthResponse provisionAdmin(String firebaseUid, String email, String displayName) {
        String normalizedEmail = email.toLowerCase().trim();
        AccountIdentity identity = identityRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> identityRepository.save(new AccountIdentity(firebaseUid, normalizedEmail)));

        assertNoExistingProfile(identity.getId());

        Admin admin = adminRepository.save(new Admin(identity, displayName));
        auditService.record(identity, "ADMIN_PROVISIONED", "ADMIN", String.valueOf(admin.getId()),
                Map.of("email", normalizedEmail));

        return resolveAuthResponse(identity);
    }

    @Transactional(readOnly = true)
    public AuthResponse resolveAuthResponse(AccountIdentity identity) {
        Long id = identity.getId();
        String token = null;
        boolean deletionPending = deletionRequestRepository != null
                && deletionRequestRepository.existsByRequesterIdAndStatus(id, DeletionRequestStatus.PENDING);

        // Check Admin first so users promoted from Buyer resolve as ADMIN
        Optional<Admin> admin = adminRepository.findByIdentityId(id);
        if (admin.isPresent()) {
            Admin a = admin.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.ADMIN, a.getId(), a.getDisplayName(), null,
                    a.getAccountStatus(), a.getVerificationStatus(),
                    false, null, null, deletionPending,
                    null, null, null, null, null, null, token);
        }

        Optional<User> buyer = userRepository.findByIdentityId(id);
        if (buyer.isPresent()) {
            User u = buyer.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.BUYER, u.getId(), u.getDisplayName(), u.getPhone(),
                    u.getAccountStatus(), u.getVerificationStatus(),
                    false, null, null, deletionPending,
                    u.getDefaultAddress(), u.getDefaultLatitude(), u.getDefaultLongitude(),
                    u.getFoodInterests(),
                    null, null, token);
        }

        Optional<DeliveryPartner> dp = deliveryPartnerRepository.findByIdentityId(id);
        if (dp.isPresent()) {
            DeliveryPartner p = dp.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.DELIVERY_PARTNER, p.getId(), p.getDisplayName(), p.getPhone(),
                    p.getAccountStatus(), p.getVerificationStatus(),
                    false, null, null, deletionPending,
                    null, null, null, null,
                    p.getVehicleDetails(), p.getServiceArea(), token);
        }

        Optional<ShopOwner> so = shopOwnerRepository.findByIdentityId(id);
        if (so.isPresent()) {
            ShopOwner o = so.get();
            Optional<Shop> shopOpt = shopRepository != null ? shopRepository.findByOwnerId(o.getId()) : Optional.empty();
            boolean hasShop = shopOpt.isPresent();
            Long shopId = shopOpt.map(Shop::getId).orElse(null);
            ShopStatus shopApprovalStatus = shopOpt.map(Shop::getApprovalStatus).orElse(null);
            VerificationStatus effectiveVerification = shopOpt.map(s -> switch (s.getApprovalStatus()) {
                case APPROVED -> s.isDeactivated() ? VerificationStatus.SUSPENDED : VerificationStatus.APPROVED;
                case PENDING -> VerificationStatus.PENDING;
                case REJECTED -> VerificationStatus.REJECTED;
                case SUSPENDED -> VerificationStatus.SUSPENDED;
            }).orElse(VerificationStatus.PENDING);

            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.SHOP_OWNER, o.getId(), o.getDisplayName(), o.getPhone(),
                    o.getAccountStatus(), effectiveVerification,
                    hasShop, shopId, shopApprovalStatus, deletionPending,
                    null, null, null, null, null, null, token);
        }

        return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(), null, null, null, null, null, token);
    }

    private void assertNoExistingProfile(Long identityId) {
        if (identityId == null) {
            return;
        }
        if (userRepository.existsByIdentityId(identityId) ||
                deliveryPartnerRepository.existsByIdentityId(identityId) ||
                shopOwnerRepository.existsByIdentityId(identityId) ||
                adminRepository.existsByIdentityId(identityId)) {
            throw new ConflictException("Account identity already has an active profile. Account-type switching is prohibited.");
        }
    }
}
