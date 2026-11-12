package com.quickbite.buyer;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.administration.AdminRoleRequest;
import com.quickbite.administration.AdminRoleRequestRepository;
import com.quickbite.administration.dto.AdminRoleRequestResponse;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.dto.UpdateProfileRequest;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AccountIdentityRepository identityRepository;
    private final AdminRepository adminRepository;
    private final AdminRoleRequestRepository adminRoleRequestRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ShopOwnerRepository shopOwnerRepository;

    @Autowired
    public AccountService(
            UserRepository userRepository,
            AuditService auditService,
            AccountIdentityRepository identityRepository,
            AdminRepository adminRepository,
            AdminRoleRequestRepository adminRoleRequestRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.identityRepository = identityRepository;
        this.adminRepository = adminRepository;
        this.adminRoleRequestRepository = adminRoleRequestRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
    }

    @Transactional
    public User updateProfile(CurrentAccountPrincipal principal, UpdateProfileRequest request) {
        User user = getUser(principal);
        user.updateProfile(request.displayName(), request.phone());
        if (request.defaultAddress() != null || request.defaultLatitude() != null || request.defaultLongitude() != null) {
            user.updateDeliveryLocation(
                    request.defaultAddress() != null ? request.defaultAddress() : user.getDefaultAddress(),
                    request.defaultLatitude() != null ? request.defaultLatitude() : user.getDefaultLatitude(),
                    request.defaultLongitude() != null ? request.defaultLongitude() : user.getDefaultLongitude()
            );
        }
        if (request.foodInterests() != null) {
            user.updateFoodInterests(request.foodInterests());
        }
        auditService.record(user.getIdentity(), "PROFILE_UPDATED", "USER", String.valueOf(user.getId()),
                Map.of("displayName", request.displayName()));
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(CurrentAccountPrincipal principal) {
        User user = getUser(principal);
        if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
            return;
        }
        user.deactivate();
        userRepository.save(user);
        auditService.record(user.getIdentity(), "ACCOUNT_DEACTIVATED", "USER", String.valueOf(user.getId()),
                Map.of("reason", "Self-deactivated by buyer"));
    }

    @Transactional
    public AdminRoleRequestResponse requestAdminRole(CurrentAccountPrincipal principal, String reason) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        AccountIdentity identity = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Account identity not found"));

        var existingAdmin = adminRepository.findByIdentityId(identity.getId());
        if (existingAdmin.isPresent()) {
            Admin admin = existingAdmin.get();
            if (admin.isVerifiedAndActive()) {
                throw new ConflictException("This account is already an active Admin");
            }
            if (admin.getAccountStatus() == AccountStatus.SUSPENDED || admin.getAccountStatus() == AccountStatus.DEACTIVATED) {
                throw new ForbiddenException("Suspended or deactivated admin accounts require security key reactivation");
            }
            admin.requestReReview(reason);
            adminRepository.save(admin);
            auditService.record(identity, "ADMIN_ACCESS_RE_REQUESTED", "ADMIN", String.valueOf(admin.getId()),
                    Map.of("email", identity.getEmail()));
            return new AdminRoleRequestResponse(
                    null,
                    identity.getId(),
                    identity.getEmail(),
                    admin.getDisplayName(),
                    AccountTypeSnapshot.ADMIN,
                    admin.getRequestNote(),
                    admin.getVerificationStatus(),
                    null,
                    admin.getCreatedAt(),
                    null
            );
        }

        if (adminRoleRequestRepository.existsByIdentityIdAndStatus(identity.getId(), VerificationStatus.PENDING)) {
            throw new ConflictException("You already have a pending request to become an Admin");
        }

        AccountTypeSnapshot currentRole = principal.getAccountType() != null
                ? principal.getAccountType()
                : AccountTypeSnapshot.BUYER;
        AdminRoleRequest saved = adminRoleRequestRepository.save(new AdminRoleRequest(identity, currentRole, reason));
        auditService.record(identity, "ADMIN_ROLE_PROMOTION_REQUESTED", "ADMIN_ROLE_REQUEST",
                String.valueOf(saved.getId()), Map.of("currentRole", currentRole.name()));
        return AdminRoleRequestResponse.from(saved, resolveDisplayName(identity));
    }

    @Transactional(readOnly = true)
    public List<AdminRoleRequestResponse> getMyAdminRequests(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        AccountIdentity identity = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Account identity not found"));
        String displayName = resolveDisplayName(identity);
        return adminRoleRequestRepository.findByIdentityIdOrderByCreatedAtDesc(identity.getId()).stream()
                .map(req -> AdminRoleRequestResponse.from(req, displayName))
                .toList();
    }

    private String resolveDisplayName(AccountIdentity identity) {
        return userRepository.findByIdentityId(identity.getId()).map(User::getDisplayName)
                .or(() -> deliveryPartnerRepository.findByIdentityId(identity.getId()).map(DeliveryPartner::getDisplayName))
                .or(() -> shopOwnerRepository.findByIdentityId(identity.getId()).map(ShopOwner::getDisplayName))
                .or(() -> adminRepository.findByIdentityId(identity.getId()).map(Admin::getDisplayName))
                .orElse(identity.getEmail());
    }

    private User getUser(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));
    }
}
