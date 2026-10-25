package com.quickbite.delivery;

import com.quickbite.audit.AuditService;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.dto.DeliveryResponse;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DeliveryService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final CustomerOrderRepository orderRepository;
    private final AuditService auditService;

    public DeliveryService(
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            CustomerOrderRepository orderRepository,
            AuditService auditService) {
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getAvailableDeliveries() {
        return deliveryAssignmentRepository.findByStatus(DeliveryStatus.UNASSIGNED).stream()
                .filter(da -> da.getOrder().getStatus() == OrderStatus.READY || da.getOrder().getStatus() == OrderStatus.PREPARING || da.getOrder().getStatus() == OrderStatus.ACCEPTED)
                .map(DeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getMyDeliveries(CurrentAccountPrincipal principal) {
        DeliveryPartner partner = getDeliveryPartner(principal);
        return deliveryAssignmentRepository.findByDeliveryPartnerIdOrderByAssignedAtDesc(partner.getId()).stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    @Transactional
    public DeliveryResponse claimAssignment(CurrentAccountPrincipal principal, Long assignmentId) {
        DeliveryPartner partner = getDeliveryPartner(principal);
        if (!partner.isActive()) {
            throw new ForbiddenException("Account is not active");
        }

        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        assignment.assignTo(partner);
        assignment.accept(partner);
        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);

        auditService.record(partner.getIdentity(), "DELIVERY_CLAIMED", "DELIVERY", String.valueOf(saved.getId()),
                Map.of("orderId", assignment.getOrder().getId()));

        return DeliveryResponse.from(saved);
    }

    @Transactional
    public DeliveryResponse markPickedUp(CurrentAccountPrincipal principal, Long assignmentId) {
        DeliveryPartner partner = getDeliveryPartner(principal);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        assignment.markPickedUp(partner);

        // Update order status to OUT_FOR_DELIVERY
        CustomerOrder order = assignment.getOrder();
        if (order.getStatus() == OrderStatus.READY || order.getStatus() == OrderStatus.PREPARING) {
            order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
            orderRepository.save(order);
        }

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        auditService.record(partner.getIdentity(), "DELIVERY_PICKED_UP", "DELIVERY", String.valueOf(saved.getId()), null);

        return DeliveryResponse.from(saved);
    }

    @Transactional
    public DeliveryResponse markDelivered(CurrentAccountPrincipal principal, Long assignmentId) {
        DeliveryPartner partner = getDeliveryPartner(principal);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        assignment.markDelivered(partner);

        // Synchronize customer order status to DELIVERED
        CustomerOrder order = assignment.getOrder();
        order.transitionTo(OrderStatus.DELIVERED);
        orderRepository.save(order);

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        auditService.record(partner.getIdentity(), "DELIVERY_COMPLETED", "DELIVERY", String.valueOf(saved.getId()), null);

        return DeliveryResponse.from(saved);
    }

    @Transactional
    public DeliveryResponse reportFailure(CurrentAccountPrincipal principal, Long assignmentId, String reason) {
        DeliveryPartner partner = getDeliveryPartner(principal);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        assignment.reportFailure(partner, reason);
        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);

        auditService.record(partner.getIdentity(), "DELIVERY_FAILED", "DELIVERY", String.valueOf(saved.getId()),
                Map.of("reason", reason != null ? reason : ""));

        return DeliveryResponse.from(saved);
    }

    private DeliveryPartner getDeliveryPartner(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return deliveryPartnerRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only registered delivery partners can perform this action"));
    }
}
