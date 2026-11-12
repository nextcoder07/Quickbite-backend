package com.quickbite.administration;

import com.quickbite.accountdeletion.AccountDeletionRequestRepository;
import com.quickbite.administration.dto.AdminAccountResponse;
import com.quickbite.administration.dto.AdminDashboardSummary;
import com.quickbite.administration.dto.AdminRoleRequestResponse;
import com.quickbite.administration.dto.AuditLogResponse;
import com.quickbite.administration.dto.DeliveryPartnerAdminResponse;
import com.quickbite.audit.AuditLogRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.DeletionRequestStatus;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.enums.DisputeStatus;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryAssignment;
import com.quickbite.delivery.DeliveryAssignmentRepository;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.dispute.DisputeRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
import com.quickbite.shop.ShopRepository;
import com.quickbite.shop.dto.ShopResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final ShopRepository shopRepository;
    private final CustomerOrderRepository orderRepository;
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final DisputeRepository disputeRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminRepository adminRepository;
    private final AuditService auditService;
    private final AdminRoleRequestRepository adminRoleRequestRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final AdminKeyValidator adminKeyValidator;

    @Autowired
    public AdminService(
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            ShopRepository shopRepository,
            CustomerOrderRepository orderRepository,
            AccountDeletionRequestRepository deletionRequestRepository,
            DisputeRepository disputeRepository,
            AuditLogRepository auditLogRepository,
            AdminRepository adminRepository,
            AuditService auditService,
            AdminRoleRequestRepository adminRoleRequestRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminKeyValidator adminKeyValidator) {
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.shopRepository = shopRepository;
        this.orderRepository = orderRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.disputeRepository = disputeRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
        this.adminRoleRequestRepository = adminRoleRequestRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.adminKeyValidator = adminKeyValidator;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummary getSummary(CurrentAccountPrincipal principal) {
        getAdmin(principal);
        return getSummary();
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummary getSummary() {
        long totalUsers = userRepository.count();
        long totalDeliveryPartners = deliveryPartnerRepository.count();
        long totalShops = shopRepository.count();
        List<CustomerOrder> orders = orderRepository.findAll();
        long totalOrders = orders.size();

        BigDecimal totalVolume = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.ACCEPTED || o.getStatus() == OrderStatus.READY || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingShops = shopRepository.findByApprovalStatus(ShopStatus.PENDING).size();
        long pendingDeliveryPartners = deliveryPartnerRepository.findAll().stream()
                .filter(p -> p.getVerificationStatus() == VerificationStatus.PENDING && p.isActive())
                .count();
        long pendingDeletions = deletionRequestRepository.findByStatus(DeletionRequestStatus.PENDING).size();
        long openDisputes = disputeRepository.findByStatus(DisputeStatus.OPEN).size() +
                disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW).size();

        long pendingAdminSignups = adminRepository.findAll().stream()
                .filter(a -> a.getVerificationStatus() == VerificationStatus.PENDING && a.isActive())
                .count();
        long pendingAdminPromotions = adminRoleRequestRepository != null
                ? adminRoleRequestRepository.countByStatus(VerificationStatus.PENDING)
                : 0;

        return new AdminDashboardSummary(
                totalUsers,
                totalDeliveryPartners,
                totalShops,
                totalOrders,
                totalVolume,
                pendingShops,
                pendingDeliveryPartners,
                pendingDeletions,
                openDisputes,
                pendingAdminSignups + pendingAdminPromotions
        );
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops(CurrentAccountPrincipal principal) {
        getAdmin(principal);
        return shopRepository.findAll().stream()
                .map(ShopResponse::from)
                .toList();
    }

    @Transactional
    public ShopResponse reviewShop(CurrentAccountPrincipal principal, Long shopId, ShopStatus newStatus) {
        Admin admin = getAdmin(principal);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        switch (newStatus) {
            case APPROVED -> shop.approve();
            case REJECTED -> shop.reject();
            case SUSPENDED -> shop.suspend();
            default -> throw new IllegalArgumentException("Invalid target shop approval status: " + newStatus);
        }

        Shop saved = shopRepository.save(shop);
        auditService.record(admin.getIdentity(), "SHOP_STATUS_CHANGED", "SHOP", String.valueOf(shop.getId()),
                Map.of("newStatus", newStatus.name()));

        return ShopResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(CurrentAccountPrincipal principal) {
        getAdmin(principal);
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryPartnerAdminResponse> getAllDeliveryPartners(CurrentAccountPrincipal principal) {
        getAdmin(principal);
        List<DeliveryPartner> partners = deliveryPartnerRepository.findAll();
        return partners.stream().map(p -> {
            List<DeliveryAssignment> assignments = deliveryAssignmentRepository
                    .findByDeliveryPartnerIdOrderByAssignedAtDesc(p.getId());
            long completed = assignments.stream().filter(a -> a.getStatus() == DeliveryStatus.DELIVERED).count();
            long active = assignments.stream().filter(a -> a.getStatus() == DeliveryStatus.ACCEPTED || a.getStatus() == DeliveryStatus.PICKED_UP).count();
            return DeliveryPartnerAdminResponse.of(p, completed, active);
        }).toList();
    }

    @Transactional
    public ShopResponse deactivateShop(CurrentAccountPrincipal principal, Long shopId) {
        Admin admin = getAdmin(principal);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));
        shop.deactivate();
        Shop saved = shopRepository.save(shop);
        auditService.record(admin.getIdentity(), "SHOP_REMOVED_BY_ADMIN", "SHOP", String.valueOf(shop.getId()),
                Map.of("shopName", shop.getName()));
        return ShopResponse.from(saved);
    }

    @Transactional
    public DeliveryPartnerAdminResponse reviewDeliveryPartnerStatus(CurrentAccountPrincipal principal, Long partnerId, String targetStatus) {
        Admin admin = getAdmin(principal);
        DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Partner not found with id: " + partnerId));

        String normalized = targetStatus != null ? targetStatus.trim().toUpperCase() : "";
        switch (normalized) {
            case "ACTIVE", "APPROVED" -> partner.approveVerification();
            case "REJECTED" -> partner.rejectVerification();
            case "SUSPENDED" -> partner.suspend();
            case "DEACTIVATED" -> partner.deactivate();
            default -> throw new IllegalArgumentException("Unsupported delivery partner status: " + targetStatus);
        }

        DeliveryPartner saved = deliveryPartnerRepository.save(partner);
        auditService.record(admin.getIdentity(), "DELIVERY_PARTNER_STATUS_CHANGED", "DELIVERY_PARTNER",
                String.valueOf(saved.getId()), Map.of("newStatus", normalized, "verificationStatus", saved.getVerificationStatus().name()));

        List<DeliveryAssignment> assignments = deliveryAssignmentRepository
                .findByDeliveryPartnerIdOrderByAssignedAtDesc(saved.getId());
        long completed = assignments.stream().filter(a -> a.getStatus() == DeliveryStatus.DELIVERED).count();
        long active = assignments.stream().filter(a -> a.getStatus() == DeliveryStatus.ACCEPTED || a.getStatus() == DeliveryStatus.PICKED_UP).count();

        return DeliveryPartnerAdminResponse.of(saved, completed, active);
    }

    @Transactional
    public DeliveryPartnerAdminResponse reviewDeliveryPartnerStatus(CurrentAccountPrincipal principal, Long partnerId, AccountStatus newStatus) {
        return reviewDeliveryPartnerStatus(principal, partnerId, newStatus.name());
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAllAdmins(CurrentAccountPrincipal principal) {
        getAdmin(principal);
        return adminRepository.findAll().stream()
                .map(AdminAccountResponse::from)
                .toList();
    }

    @Transactional
    public AdminAccountResponse reviewAdminStatus(CurrentAccountPrincipal principal, Long targetAdminId, String targetStatus, String adminKey) {
        Admin actingAdmin = getAdmin(principal);
        Admin targetAdmin = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + targetAdminId));

        String normalized = targetStatus != null ? targetStatus.trim().toUpperCase() : "";
        switch (normalized) {
            case "SUSPENDED", "DEACTIVATED" -> {
                if (targetAdmin.getId().equals(actingAdmin.getId())) {
                    throw new BadRequestException("You cannot suspend your own admin account");
                }
                adminKeyValidator.requireValid(adminKey, "Valid admin security key is required to suspend an admin");
                if ("DEACTIVATED".equals(normalized)) {
                    targetAdmin.deactivate();
                } else {
                    targetAdmin.suspend();
                }
            }
            case "APPROVED", "ACTIVE" -> {
                if (targetAdmin.getAccountStatus() == AccountStatus.SUSPENDED
                        || targetAdmin.getAccountStatus() == AccountStatus.DEACTIVATED) {
                    adminKeyValidator.requireValid(adminKey, "Valid admin security key is required to reactivate a suspended admin");
                }
                targetAdmin.approveVerification();
            }
            case "REJECTED" -> {
                if (targetAdmin.getId().equals(actingAdmin.getId())) {
                    throw new BadRequestException("You cannot reject your own admin account");
                }
                targetAdmin.rejectVerification();
            }
            default -> throw new BadRequestException("Unsupported admin status: " + targetStatus);
        }

        Admin saved = adminRepository.save(targetAdmin);
        auditService.record(actingAdmin.getIdentity(), "ADMIN_STATUS_CHANGED", "ADMIN",
                String.valueOf(saved.getId()),
                Map.of("newStatus", normalized, "verificationStatus", saved.getVerificationStatus().name()));
        return AdminAccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminRoleRequestResponse> getAdminRoleRequests(CurrentAccountPrincipal principal) {
        getAdmin(principal);
        return adminRoleRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(req -> AdminRoleRequestResponse.from(req, resolveDisplayName(req.getIdentity())))
                .toList();
    }

    @Transactional
    public AdminRoleRequestResponse reviewAdminRoleRequest(CurrentAccountPrincipal principal, Long requestId, boolean approve, String note) {
        Admin actingAdmin = getAdmin(principal);
        AdminRoleRequest req = adminRoleRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin role request not found with id: " + requestId));

        if (req.getStatus() != VerificationStatus.PENDING) {
            throw new BadRequestException("Admin role request has already been reviewed");
        }

        AccountIdentity identity = req.getIdentity();
        String displayName = resolveDisplayName(identity);

        if (approve) {
            Admin promoted = adminRepository.findByIdentityId(identity.getId()).orElse(null);
            if (promoted == null) {
                promoted = new Admin(identity, displayName, VerificationStatus.APPROVED, req.getReason());
            } else {
                promoted.approveVerification();
            }
            adminRepository.save(promoted);
            req.approve(actingAdmin, note);
            auditService.record(actingAdmin.getIdentity(), "ADMIN_ROLE_PROMOTION_APPROVED", "ADMIN_ROLE_REQUEST",
                    String.valueOf(req.getId()), Map.of("targetEmail", identity.getEmail()));
        } else {
            req.reject(actingAdmin, note);
            auditService.record(actingAdmin.getIdentity(), "ADMIN_ROLE_PROMOTION_REJECTED", "ADMIN_ROLE_REQUEST",
                    String.valueOf(req.getId()), Map.of("targetEmail", identity.getEmail()));
        }

        AdminRoleRequest saved = adminRoleRequestRepository.save(req);
        return AdminRoleRequestResponse.from(saved, displayName);
    }

    private String resolveDisplayName(AccountIdentity identity) {
        return userRepository.findByIdentityId(identity.getId()).map(User::getDisplayName)
                .or(() -> deliveryPartnerRepository.findByIdentityId(identity.getId()).map(DeliveryPartner::getDisplayName))
                .or(() -> shopOwnerRepository.findByIdentityId(identity.getId()).map(ShopOwner::getDisplayName))
                .or(() -> adminRepository.findByIdentityId(identity.getId()).map(Admin::getDisplayName))
                .orElse(identity.getEmail());
    }

    private Admin getAdmin(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        Admin admin = adminRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only admins can perform administrative workflows"));
        if (!admin.canOperateOnPlatform()) {
            throw new ForbiddenException("Admin account is pending approval or suspended");
        }
        return admin;
    }
}
