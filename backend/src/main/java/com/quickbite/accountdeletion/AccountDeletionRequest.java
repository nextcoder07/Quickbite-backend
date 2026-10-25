package com.quickbite.accountdeletion;

import com.quickbite.administration.Admin;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.DeletionRequestStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.InvalidStateTransitionException;
import com.quickbite.identity.AccountIdentity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "account_deletion_requests")
public class AccountDeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_identity_id", nullable = false)
    private AccountIdentity requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_type", nullable = false, length = 30)
    private AccountTypeSnapshot requesterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeletionRequestStatus status;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private Admin reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AccountDeletionRequest() {}

    public AccountDeletionRequest(AccountIdentity requester, AccountTypeSnapshot requesterType, String reason) {
        this.requester = Objects.requireNonNull(requester, "requester must not be null");
        this.requesterType = Objects.requireNonNull(requesterType, "requesterType must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null").trim();
        this.status = DeletionRequestStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = DeletionRequestStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public AccountIdentity getRequester() {
        return requester;
    }

    public AccountTypeSnapshot getRequesterType() {
        return requesterType;
    }

    public DeletionRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Admin getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void confirm(Admin admin, String reviewNote) {
        validateReviewer(admin);
        if (this.status != DeletionRequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only pending deletion requests can be confirmed");
        }
        this.reviewedBy = admin;
        this.reviewNote = reviewNote != null ? reviewNote.trim() : null;
        this.status = DeletionRequestStatus.CONFIRMED;
        this.reviewedAt = Instant.now();
        this.completedAt = Instant.now();
    }

    public void decline(Admin admin, String reviewNote) {
        validateReviewer(admin);
        if (this.status != DeletionRequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only pending deletion requests can be declined");
        }
        this.reviewedBy = admin;
        this.reviewNote = reviewNote != null ? reviewNote.trim() : null;
        this.status = DeletionRequestStatus.DECLINED;
        this.reviewedAt = Instant.now();
    }

    public void cancel() {
        if (this.status != DeletionRequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Only pending deletion requests can be cancelled");
        }
        this.status = DeletionRequestStatus.CANCELLED;
    }

    private void validateReviewer(Admin admin) {
        Objects.requireNonNull(admin, "admin reviewer must not be null");
        if (admin.getIdentity().getId().equals(this.requester.getId())) {
            throw new ForbiddenException("Self-review is strictly prohibited for deletion requests");
        }
    }
}
