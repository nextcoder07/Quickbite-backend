package com.quickbite.dispute;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.dispute.dto.CreateDisputeRequest;
import com.quickbite.dispute.dto.DisputeResponse;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final CustomerOrderRepository orderRepository;
    private final AccountIdentityRepository identityRepository;
    private final AdminRepository adminRepository;
    private final AuditService auditService;

    public DisputeService(
            DisputeRepository disputeRepository,
            CustomerOrderRepository orderRepository,
            AccountIdentityRepository identityRepository,
            AdminRepository adminRepository,
            AuditService auditService) {
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.identityRepository = identityRepository;
        this.adminRepository = adminRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DisputeResponse raiseDispute(CurrentAccountPrincipal principal, CreateDisputeRequest request) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }

        CustomerOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

        if (!order.getBuyer().getIdentity().getId().equals(principal.getIdentityId())) {
            throw new ForbiddenException("You can only raise disputes for orders placed by your account");
        }

        AccountIdentity raisedBy = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Identity not found"));

        Dispute dispute = new Dispute(order, raisedBy, request.category(), request.description());
        Dispute saved = disputeRepository.save(dispute);

        auditService.record(raisedBy, "DISPUTE_RAISED", "DISPUTE", String.valueOf(saved.getId()),
                Map.of("orderId", order.getId(), "category", request.category()));

        return DisputeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> getMyDisputes(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return disputeRepository.findByRaisedById(principal.getIdentityId()).stream()
                .map(DisputeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> getAllDisputes() {
        return disputeRepository.findAll().stream()
                .map(DisputeResponse::from)
                .toList();
    }

    @Transactional
    public DisputeResponse resolveDispute(CurrentAccountPrincipal principal, Long disputeId, boolean resolve, String note) {
        Admin admin = getAdmin(principal);

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id: " + disputeId));

        if (resolve) {
            dispute.resolve(admin, note);
        } else {
            dispute.reject(admin, note);
        }

        Dispute saved = disputeRepository.save(dispute);

        auditService.record(admin.getIdentity(), "DISPUTE_RESOLVED", "DISPUTE", String.valueOf(saved.getId()),
                Map.of("resolved", resolve, "note", note != null ? note : ""));

        return DisputeResponse.from(saved);
    }

    private Admin getAdmin(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return adminRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only admins can resolve disputes"));
    }
}
