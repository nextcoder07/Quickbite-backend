package com.quickbite.delivery;

import com.quickbite.audit.AuditService;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.InvalidStateTransitionException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.dto.DeliveryResponse;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    public List<DeliveryResponse> getAvailableDeliveries(CurrentAccountPrincipal principal) {
        getVerifiedDeliveryPartner(principal);
        return getAvailableDeliveries();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getAvailableDeliveries() {
        return deliveryAssignmentRepository.findByStatus(DeliveryStatus.UNASSIGNED).stream()
                .filter(da -> da.getDeliveryPartner() == null)
                .filter(da -> da.getOrder().getStatus() == OrderStatus.READY
                        || da.getOrder().getStatus() == OrderStatus.PREPARING
                        || da.getOrder().getStatus() == OrderStatus.ACCEPTED)
                .map(DeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getMyDeliveries(CurrentAccountPrincipal principal) {
        DeliveryPartner partner = getVerifiedDeliveryPartner(principal);
        return deliveryAssignmentRepository.findByDeliveryPartnerIdOrderByAssignedAtDesc(partner.getId()).stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    /**
     * Atomically claims an available delivery assignment with both database PESSIMISTIC_WRITE row lock
     * and an atomic conditional UPDATE verified from the database so no two delivery partners
     * can accept the same order simultaneously.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DeliveryResponse claimAssignment(CurrentAccountPrincipal principal, Long assignmentId) {
        DeliveryPartner partner = getVerifiedDeliveryPartner(principal);

        try {
            DeliveryAssignment locked = deliveryAssignmentRepository.findByIdForUpdate(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

            OrderStatus orderStatus = locked.getOrder().getStatus();
            if (orderStatus != OrderStatus.ACCEPTED
                    && orderStatus != OrderStatus.PREPARING
                    && orderStatus != OrderStatus.READY) {
                throw new InvalidStateTransitionException("Order is not available for delivery acceptance (status: " + orderStatus + ")");
            }

            if (locked.getStatus() != DeliveryStatus.UNASSIGNED || locked.getDeliveryPartner() != null) {
                throw new ConflictException("This delivery assignment has already been claimed by another delivery partner");
            }

            Instant now = Instant.now();
            int updatedRows = deliveryAssignmentRepository.claimAtomically(
                    assignmentId,
                    partner,
                    DeliveryStatus.UNASSIGNED,
                    DeliveryStatus.ACCEPTED,
                    now
            );

            if (updatedRows != 1) {
                throw new ConflictException("This delivery assignment was just claimed by another delivery partner");
            }

            DeliveryAssignment verified = deliveryAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found after claim"));

            if (verified.getDeliveryPartner() == null
                    || !verified.getDeliveryPartner().getId().equals(partner.getId())
                    || verified.getStatus() != DeliveryStatus.ACCEPTED) {
                throw new ConflictException("Delivery claim conflict detected while verifying database state");
            }

            auditService.record(partner.getIdentity(), "DELIVERY_CLAIMED", "DELIVERY", String.valueOf(verified.getId()),
                    Map.of("orderId", verified.getOrder().getId(), "partnerId", partner.getId()));

            return DeliveryResponse.from(verified);
        } catch (PessimisticLockingFailureException | OptimisticLockingFailureException ex) {
            throw new ConflictException("This delivery assignment was just claimed by another delivery partner");
        } catch (DataAccessException ex) {
            throw new ConflictException("Concurrent claim prevented by database lock");
        }
    }

    @Transactional
    public DeliveryResponse markPickedUp(CurrentAccountPrincipal principal, Long assignmentId) {
        DeliveryPartner partner = getVerifiedDeliveryPartner(principal);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        CustomerOrder order = assignment.getOrder();
        if (order.getStatus() != OrderStatus.READY) {
            throw new InvalidStateTransitionException("Order must be ready before it can be picked up");
        }
        assignment.markPickedUp(partner);
        order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        auditService.record(partner.getIdentity(), "DELIVERY_PICKED_UP", "DELIVERY", String.valueOf(saved.getId()), null);

        return DeliveryResponse.from(saved);
    }

    @Transactional
    public DeliveryResponse markDelivered(CurrentAccountPrincipal principal, Long assignmentId, String otp) {
        DeliveryPartner partner = getVerifiedDeliveryPartner(principal);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        CustomerOrder order = assignment.getOrder();
        assignment.markDelivered(partner, otp);
        orderRepository.save(order);

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        auditService.record(partner.getIdentity(), "DELIVERY_COMPLETED", "DELIVERY", String.valueOf(saved.getId()), null);

        return DeliveryResponse.from(saved);
    }

    @Transactional
    public DeliveryResponse reportFailure(CurrentAccountPrincipal principal, Long assignmentId, String reason) {
        DeliveryPartner partner = getVerifiedDeliveryPartner(principal);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        assignment.reportFailure(partner, reason);
        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);

        auditService.record(partner.getIdentity(), "DELIVERY_FAILED", "DELIVERY", String.valueOf(saved.getId()),
                Map.of("reason", reason != null ? reason : ""));

        return DeliveryResponse.from(saved);
    }

    private DeliveryPartner getVerifiedDeliveryPartner(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        DeliveryPartner partner = deliveryPartnerRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only registered delivery partners can perform this action"));
        if (!partner.isVerifiedAndActive()) {
            throw new ForbiddenException("Delivery partner account must be verified by admin and active to access deliveries");
        }
        return partner;
    }
}
