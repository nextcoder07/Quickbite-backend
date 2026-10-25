package com.quickbite.dispute;

import com.quickbite.administration.Admin;
import com.quickbite.common.enums.DisputeStatus;
import com.quickbite.common.exception.InvalidStateTransitionException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.ordering.CustomerOrder;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raised_by_identity_id", nullable = false)
    private AccountIdentity raisedBy;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DisputeStatus status;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_admin_id")
    private Admin resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Dispute() {}

    public Dispute(CustomerOrder order, AccountIdentity raisedBy, String category, String description) {
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.raisedBy = Objects.requireNonNull(raisedBy, "raisedBy must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null").trim();
        this.description = Objects.requireNonNull(description, "description must not be null").trim();
        this.status = DisputeStatus.OPEN;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = DisputeStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public AccountIdentity getRaisedBy() {
        return raisedBy;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Admin getResolvedBy() {
        return resolvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void markUnderReview() {
        if (this.status != DisputeStatus.OPEN) {
            throw new InvalidStateTransitionException("Only open disputes can be marked under review");
        }
        this.status = DisputeStatus.UNDER_REVIEW;
    }

    public void resolve(Admin admin, String resolutionNote) {
        if (this.status == DisputeStatus.RESOLVED || this.status == DisputeStatus.CLOSED) {
            throw new InvalidStateTransitionException("Dispute is already completed");
        }
        this.resolvedBy = Objects.requireNonNull(admin, "admin must not be null");
        this.resolutionNote = Objects.requireNonNull(resolutionNote, "resolutionNote must not be null").trim();
        this.status = DisputeStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public void reject(Admin admin, String resolutionNote) {
        if (this.status == DisputeStatus.RESOLVED || this.status == DisputeStatus.CLOSED) {
            throw new InvalidStateTransitionException("Dispute is already completed");
        }
        this.resolvedBy = Objects.requireNonNull(admin, "admin must not be null");
        this.resolutionNote = Objects.requireNonNull(resolutionNote, "resolutionNote must not be null").trim();
        this.status = DisputeStatus.REJECTED;
        this.resolvedAt = Instant.now();
    }

    public void close() {
        this.status = DisputeStatus.CLOSED;
        this.resolvedAt = Instant.now();
    }
}
