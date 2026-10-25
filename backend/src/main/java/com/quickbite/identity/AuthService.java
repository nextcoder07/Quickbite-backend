package com.quickbite.identity;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
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

    public AuthService(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            AuditService auditService) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse registerProfile(SignupRequest request) {
        if (request.accountType() == AccountTypeSnapshot.ADMIN) {
            throw new ForbiddenException("Public signup for Admin accounts is strictly prohibited");
        }

        String normalizedEmail = request.email().toLowerCase().trim();

        // 1. Find or create AccountIdentity
        AccountIdentity identity = identityRepository.findByFirebaseUid(request.firebaseUid())
                .orElseGet(() -> identityRepository.findByEmailIgnoreCase(normalizedEmail)
                        .orElseGet(() -> identityRepository.save(new AccountIdentity(request.firebaseUid(), normalizedEmail))));

        // 2. Enforce strict profile exclusivity
        assertNoExistingProfile(identity.getId());

        Long profileId = null;
        AccountStatus status = AccountStatus.ACTIVE;

        switch (request.accountType()) {
            case BUYER -> {
                User buyer = userRepository.save(new User(identity, request.displayName(), request.phone()));
                profileId = buyer.getId();
                status = buyer.getAccountStatus();
            }
            case DELIVERY_PARTNER -> {
                DeliveryPartner dp = deliveryPartnerRepository.save(new DeliveryPartner(identity, request.displayName(), request.phone()));
                profileId = dp.getId();
                status = dp.getAccountStatus();
            }
            case SHOP_OWNER -> {
                ShopOwner so = shopOwnerRepository.save(new ShopOwner(identity, request.displayName(), request.phone()));
                profileId = so.getId();
                status = so.getAccountStatus();
            }
            default -> throw new ForbiddenException("Unsupported account profile type");
        }

        auditService.record(identity, "PROFILE_CREATED", request.accountType().name(), String.valueOf(profileId),
                Map.of("email", identity.getEmail(), "role", request.accountType().name()));

        String devToken = "dev-token-" + identity.getEmail();
        return new AuthResponse(
                identity.getId(),
                identity.getFirebaseUid(),
                identity.getEmail(),
                request.accountType(),
                profileId,
                request.displayName(),
                request.phone(),
                status,
                devToken
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ResourceNotFoundException("Authenticated identity not found");
        }

        AccountIdentity identity = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Account identity not found"));

        return resolveAuthResponse(identity);
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

        return new AuthResponse(
                identity.getId(),
                identity.getFirebaseUid(),
                identity.getEmail(),
                AccountTypeSnapshot.ADMIN,
                admin.getId(),
                admin.getDisplayName(),
                null,
                admin.getAccountStatus(),
                "dev-token-" + identity.getEmail()
        );
    }

    @Transactional
    public AuthResponse resolveAuthResponse(AccountIdentity identity) {
        Long id = identity.getId();

        Optional<User> buyer = userRepository.findByIdentityId(id);
        if (buyer.isPresent()) {
            User u = buyer.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.BUYER, u.getId(), u.getDisplayName(), u.getPhone(), u.getAccountStatus(), "dev-token-" + identity.getEmail());
        }

        Optional<DeliveryPartner> dp = deliveryPartnerRepository.findByIdentityId(id);
        if (dp.isPresent()) {
            DeliveryPartner p = dp.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.DELIVERY_PARTNER, p.getId(), p.getDisplayName(), p.getPhone(), p.getAccountStatus(), "dev-token-" + identity.getEmail());
        }

        Optional<ShopOwner> so = shopOwnerRepository.findByIdentityId(id);
        if (so.isPresent()) {
            ShopOwner o = so.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.SHOP_OWNER, o.getId(), o.getDisplayName(), o.getPhone(), o.getAccountStatus(), "dev-token-" + identity.getEmail());
        }

        Optional<Admin> admin = adminRepository.findByIdentityId(id);
        if (admin.isPresent()) {
            Admin a = admin.get();
            return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.ADMIN, a.getId(), a.getDisplayName(), null, a.getAccountStatus(), "dev-token-" + identity.getEmail());
        }

        return new AuthResponse(id, identity.getFirebaseUid(), identity.getEmail(), null, null, null, null, null, "dev-token-" + identity.getEmail());
    }

    private void assertNoExistingProfile(Long identityId) {
        if (userRepository.existsByIdentityId(identityId) ||
                deliveryPartnerRepository.existsByIdentityId(identityId) ||
                shopOwnerRepository.existsByIdentityId(identityId) ||
                adminRepository.existsByIdentityId(identityId)) {
            throw new ConflictException("Account identity already has an active profile. Account-type switching is prohibited.");
        }
    }
}
