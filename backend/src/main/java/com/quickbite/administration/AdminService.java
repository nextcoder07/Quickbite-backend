package com.quickbite.administration;

import com.quickbite.accountdeletion.AccountDeletionRequestRepository;
import com.quickbite.administration.dto.AdminDashboardSummary;
import com.quickbite.administration.dto.AuditLogResponse;
import com.quickbite.audit.AuditLogRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.DeletionRequestStatus;
import com.quickbite.common.enums.DisputeStatus;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.dispute.DisputeRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopRepository;
import com.quickbite.shop.dto.ShopResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ShopRepository shopRepository;
    private final CustomerOrderRepository orderRepository;
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final DisputeRepository disputeRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminRepository adminRepository;
    private final AuditService auditService;

    public AdminService(
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopRepository shopRepository,
            CustomerOrderRepository orderRepository,
            AccountDeletionRequestRepository deletionRequestRepository,
            DisputeRepository disputeRepository,
            AuditLogRepository auditLogRepository,
            AdminRepository adminRepository,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopRepository = shopRepository;
        this.orderRepository = orderRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.disputeRepository = disputeRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
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
        long pendingDeletions = deletionRequestRepository.findByStatus(DeletionRequestStatus.PENDING).size();
        long openDisputes = disputeRepository.findByStatus(DisputeStatus.OPEN).size() +
                disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW).size();

        return new AdminDashboardSummary(
                totalUsers,
                totalDeliveryPartners,
                totalShops,
                totalOrders,
                totalVolume,
                pendingShops,
                pendingDeletions,
                openDisputes
        );
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops() {
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
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private Admin getAdmin(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return adminRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only admins can perform administrative workflows"));
    }
}
