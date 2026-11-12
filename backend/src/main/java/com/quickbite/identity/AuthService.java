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
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.LoginRequest;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
import com.quickbite.shop.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final AccountIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final AdminRepository adminRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final ShopRepository shopRepository;
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final AdminKeyValidator adminKeyValidator;

    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService) {
        this(identityRepository, userRepository, deliveryPartnerRepository, shopOwnerRepository,
                adminRepository, auditService, new BCryptPasswordEncoder(), null, null, new AdminKeyValidator(""));
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
                adminRepository, auditService, new BCryptPasswordEncoder(), null, null, adminKeyValidator);
    }

    @Autowired
    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            ShopRepository shopRepository,
            AccountDeletionRequestRepository deletionRequestRepository,
            AdminKeyValidator adminKeyValidator) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.shopRepository = shopRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.adminKeyValidator = adminKeyValidator != null ? adminKeyValidator : new AdminKeyValidator("");
    }

    @Transactional
    public AuthResponse registerProfile(SignupRequest request) {
        boolean hasAdminKey = request.adminKey() != null && !request.adminKey().isBlank();
        VerificationStatus adminInitialVerification = VerificationStatus.PENDING;

        if (request.accountType() == AccountTypeSnapshot.ADMIN && hasAdminKey) {
            adminKeyValidator.requireValid(request.adminKey(), "Invalid admin security key provided");
            adminInitialVerification = VerificationStatus.APPROVED;
        }

        String normalizedEmail = request.email().toLowerCase().trim();
        String uid = (request.firebaseUid() != null && !request.firebaseUid().isBlank())
                ? request.firebaseUid().trim()
                : "uid-" + UUID.randomUUID().toString().substring(0, 12);

        Optional<AccountIdentity> existingByUid = identityRepository.findByFirebaseUid(uid);
        Optional<AccountIdentity> existingByEmail = identityRepository.findByEmailIgnoreCase(normalizedEmail);

        AccountIdentity identity;
        if (existingByUid.isPresent()) {
            identity = existingByUid.get();
        } else if (existingByEmail.isPresent()) {
            identity = existingByEmail.get();
        } else {
            AccountIdentity newIdentity = new AccountIdentity(uid, normalizedEmail);
            if (request.password() != null && !request.password().isBlank()) {
                newIdentity.setPasswordHash(passwordEncoder.encode(request.password()));
            }
            identity = identityRepository.saveAndFlush(newIdentity);
        }

        if (identity.getId() != null) {
            assertNoExistingProfile(identity.getId());
        }

        if (request.password() != null && !request.password().isBlank() &&
                (identity.getPasswordHash() == null || identity.getPasswordHash().isBlank())) {
            identity.setPasswordHash(passwordEncoder.encode(request.password()));
            identity = identityRepository.saveAndFlush(identity);
        }

        Long profileId;
        switch (request.accountType()) {
            case BUYER -> {
                User newBuyer = new User(identity, request.displayName(), request.phone());
                if (request.defaultAddress() != null || request.defaultLatitude() != null || request.defaultLongitude() != null) {
                    newBuyer.updateDeliveryLocation(request.defaultAddress(), request.defaultLatitude(), request.defaultLongitude());
                }
                if (request.foodInterests() != null && !request.foodInterests().isBlank()) {
                    newBuyer.updateFoodInterests(request.foodInterests());
                }
                User buyer = userRepository.saveAndFlush(newBuyer);
                profileId = buyer.getId();
            }
            case DELIVERY_PARTNER -> {
                DeliveryPartner dp = new DeliveryPartner(
                        identity,
                        request.displayName(),
                        request.phone(),
                        request.vehicleDetails(),
                        request.serviceArea()
                );
                dp = deliveryPartnerRepository.saveAndFlush(dp);
                profileId = dp.getId();
            }
            case SHOP_OWNER -> {
                ShopOwner so = shopOwnerRepository.saveAndFlush(new ShopOwner(identity, request.displayName(), request.phone()));
                profileId = so.getId();
            }
            case ADMIN -> {
                Admin admin = adminRepository.saveAndFlush(new Admin(
                        identity,
                        request.displayName(),
                        adminInitialVerification,
                        request.adminRequestNote()
                ));
                profileId = admin.getId();
            }
            default -> throw new ForbiddenException("Unsupported account profile type");
        }

        String auditAction = request.accountType() == AccountTypeSnapshot.ADMIN
                ? (adminInitialVerification == VerificationStatus.APPROVED ? "ADMIN_CREATED_WITH_KEY" : "ADMIN_ACCESS_REQUESTED")
                : "PROFILE_CREATED";

        auditService.record(identity, auditAction, request.accountType().name(), String.valueOf(profileId),
                Map.of("email", identity.getEmail(), "role", request.accountType().name()));

        return resolveAuthResponse(identity);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        AccountIdentity identity = identityRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ForbiddenException("Invalid email or password"));

        if (identity.getPasswordHash() != null && !identity.getPasswordHash().isBlank()) {
            if (request.password() == null || !passwordEncoder.matches(request.password(), identity.getPasswordHash())) {
                throw new ForbiddenException("Invalid email or password");
            }
        }

        AuthResponse response = resolveAuthResponse(identity);
        if (response.status() == AccountStatus.DEACTIVATED) {
            throw new ForbiddenException("This account has been deactivated and removed from the platform");
        }

        auditService.record(identity, "USER_LOGIN", response.accountType() != null ? response.accountType().name() : "IDENTITY",
                String.valueOf(response.profileId()), Map.of("email", normalizedEmail));

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
        String token = "dev-token-" + identity.getEmail();
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
