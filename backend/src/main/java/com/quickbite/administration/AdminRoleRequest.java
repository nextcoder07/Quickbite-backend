package com.quickbite.administration;

import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.identity.AccountIdentity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "admin_role_requests")
public class AdminRoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_id", nullable = false)
    private AccountIdentity identity;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_role", nullable = false, length = 30)
    private AccountTypeSnapshot currentRole;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private Admin reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AdminRoleRequest() {}

    public AdminRoleRequest(AccountIdentity identity, AccountTypeSnapshot currentRole, String reason) {
        this.identity = Objects.requireNonNull(identity, "identity must not be null");
        this.currentRole = Objects.requireNonNull(currentRole, "currentRole must not be null");
        this.reason = reason != null ? reason.trim() : null;
        this.status = VerificationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = VerificationStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public AccountIdentity getIdentity() {
        return identity;
    }

    public AccountTypeSnapshot getCurrentRole() {
        return currentRole;
    }

    public String getReason() {
        return reason;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public Admin getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void approve(Admin admin, String note) {
        this.status = VerificationStatus.APPROVED;
        this.reviewedBy = admin;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void reject(Admin admin, String note) {
        this.status = VerificationStatus.REJECTED;
        this.reviewedBy = admin;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }
}

