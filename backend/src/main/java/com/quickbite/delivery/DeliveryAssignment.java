package com.quickbite.delivery;

import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.InvalidStateTransitionException;
import com.quickbite.ordering.CustomerOrder;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_partner_id")
    private DeliveryPartner deliveryPartner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeliveryAssignment() {}

    public DeliveryAssignment(CustomerOrder order) {
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.status = DeliveryStatus.UNASSIGNED;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public DeliveryPartner getDeliveryPartner() {
        return deliveryPartner;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assignTo(DeliveryPartner partner) {
        if (this.status != DeliveryStatus.UNASSIGNED) {
            throw new InvalidStateTransitionException("Only unassigned delivery can be assigned");
        }
        this.deliveryPartner = Objects.requireNonNull(partner, "partner must not be null");
        this.status = DeliveryStatus.ASSIGNED;
        this.assignedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void accept(DeliveryPartner partner) {
        validateAssignedPartner(partner);
        if (this.status != DeliveryStatus.ASSIGNED) {
            throw new InvalidStateTransitionException("Only assigned delivery can be accepted");
        }
        this.status = DeliveryStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markPickedUp(DeliveryPartner partner) {
        validateAssignedPartner(partner);
        if (this.status != DeliveryStatus.ACCEPTED) {
            throw new InvalidStateTransitionException("Only accepted delivery can be picked up");
        }
        this.status = DeliveryStatus.PICKED_UP;
        this.pickedUpAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markDelivered(DeliveryPartner partner) {
        validateAssignedPartner(partner);
        if (this.status != DeliveryStatus.PICKED_UP) {
            throw new InvalidStateTransitionException("Only picked up delivery can be marked delivered");
        }
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reportFailure(DeliveryPartner partner, String reason) {
        validateAssignedPartner(partner);
        if (this.status == DeliveryStatus.DELIVERED || this.status == DeliveryStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Cannot fail completed or cancelled delivery");
        }
        this.status = DeliveryStatus.FAILED;
        this.failureReason = reason != null ? reason.trim() : "Unspecified delivery issue";
        this.updatedAt = Instant.now();
    }

    private void validateAssignedPartner(DeliveryPartner partner) {
        if (this.deliveryPartner == null || partner == null || !this.deliveryPartner.getId().equals(partner.getId())) {
            throw new ForbiddenException("Operation not authorized: Delivery is not assigned to you");
        }
    }
}
