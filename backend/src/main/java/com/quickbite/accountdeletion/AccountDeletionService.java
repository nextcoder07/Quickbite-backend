package com.quickbite.accountdeletion;

import com.quickbite.accountdeletion.dto.CreateDeletionRequest;
import com.quickbite.accountdeletion.dto.DeletionRequestResponse;
import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.DeletionRequestStatus;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryAssignmentRepository;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.ShopOwnerRepository;
import com.quickbite.shop.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AccountDeletionService {

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final AccountIdentityRepository identityRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopRepository shopRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final AdminRepository adminRepository;
    private final AuditService auditService;

    public AccountDeletionService(
            AccountDeletionRequestRepository deletionRequestRepository,
            AccountIdentityRepository identityRepository,
            ShopOwnerRepository shopOwnerRepository,
            ShopRepository shopRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            AdminRepository adminRepository,
            AuditService auditService) {
        this.deletionRequestRepository = deletionRequestRepository;
        this.identityRepository = identityRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.shopRepository = shopRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DeletionRequestResponse submitRequest(CurrentAccountPrincipal principal, CreateDeletionRequest request) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }

        AccountIdentity requester = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Account identity not found"));

        AccountTypeSnapshot type = principal.getAccountType();
        if (type != AccountTypeSnapshot.SHOP_OWNER && type != AccountTypeSnapshot.DELIVERY_PARTNER) {
            throw new ForbiddenException("Platform removal requests are required for Shop Owners and Delivery Partners. Buyers can self-deactivate.");
        }

        if (deletionRequestRepository.existsByRequesterIdAndStatus(requester.getId(), DeletionRequestStatus.PENDING)) {
            throw new ConflictException("A pending removal request already exists for this account");
        }

        AccountDeletionRequest deletionRequest = new AccountDeletionRequest(requester, type, request.reason());
        AccountDeletionRequest saved = deletionRequestRepository.save(deletionRequest);

        auditService.record(requester, "DELETION_REQUEST_SUBMITTED", "ACCOUNT_DELETION_REQUEST",
                String.valueOf(saved.getId()), Map.of("requesterType", type.name()));

        return DeletionRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DeletionRequestResponse> getMyRequests(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return deletionRequestRepository.findByRequesterId(principal.getIdentityId()).stream()
                .map(DeletionRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeletionRequestResponse> getAllRequests() {
        return deletionRequestRepository.findAll().stream()
                .map(DeletionRequestResponse::from)
                .toList();
    }

    @Transactional
    public DeletionRequestResponse reviewRequest(CurrentAccountPrincipal principal, Long requestId, boolean confirm, String note) {
        Admin admin = getAdmin(principal);

        AccountDeletionRequest request = deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found with id: " + requestId));

        if (confirm) {
            if (request.getRequesterType() == AccountTypeSnapshot.SHOP_OWNER) {
                shopOwnerRepository.findByIdentityId(request.getRequester().getId()).ifPresent(so -> {
                    shopRepository.findByOwnerId(so.getId()).ifPresent(shop -> {
                        shop.deactivate();
                        shopRepository.save(shop);
                    });
                    so.deactivate();
                    shopOwnerRepository.save(so);
                });
            } else if (request.getRequesterType() == AccountTypeSnapshot.DELIVERY_PARTNER) {
                deliveryPartnerRepository.findByIdentityId(request.getRequester().getId()).ifPresent(dp -> {
                    boolean hasInTransit = deliveryAssignmentRepository
                            .findByDeliveryPartnerIdOrderByAssignedAtDesc(dp.getId())
                            .stream()
                            .anyMatch(a -> a.getStatus() == DeliveryStatus.PICKED_UP);
                    if (hasInTransit) {
                        throw new ConflictException("Cannot remove delivery partner while an order is picked up and in transit");
                    }
                    dp.deactivate();
                    deliveryPartnerRepository.save(dp);
                });
            }
            request.confirm(admin, note);
        } else {
            request.decline(admin, note);
        }

        AccountDeletionRequest saved = deletionRequestRepository.save(request);

        auditService.record(admin.getIdentity(), "DELETION_REQUEST_REVIEWED", "ACCOUNT_DELETION_REQUEST",
                String.valueOf(saved.getId()), Map.of("confirmed", confirm, "note", note != null ? note : ""));

        return DeletionRequestResponse.from(saved);
    }

    private Admin getAdmin(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        Admin admin = adminRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only admins can review removal requests"));
        if (!admin.canOperateOnPlatform()) {
            throw new ForbiddenException("Admin account is pending approval or suspended");
        }
        return admin;
    }
}
