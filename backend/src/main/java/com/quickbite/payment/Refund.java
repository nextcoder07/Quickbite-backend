package com.quickbite.payment;

import com.quickbite.administration.Admin;
import com.quickbite.common.enums.RefundStatus;
import com.quickbite.identity.AccountIdentity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_identity_id", nullable = false)
    private AccountIdentity requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private Admin reviewedBy;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "provider_refund_id", length = 100)
    private String providerRefundId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected Refund() {}

    public Refund(Payment payment, AccountIdentity requestedBy, BigDecimal amount, String reason, String idempotencyKey) {
        this.payment = Objects.requireNonNull(payment, "payment must not be null");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        this.amount = amount;
        this.reason = Objects.requireNonNull(reason, "reason must not be null").trim();
        this.idempotencyKey = idempotencyKey;
        this.status = RefundStatus.REQUESTED;
        this.requestedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = RefundStatus.REQUESTED;
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public AccountIdentity getRequestedBy() {
        return requestedBy;
    }

    public Admin getReviewedBy() {
        return reviewedBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public String getReason() {
        return reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void approve(Admin admin) {
        this.reviewedBy = Objects.requireNonNull(admin, "admin reviewer must not be null");
        this.status = RefundStatus.APPROVED;
    }

    public void reject(Admin admin) {
        this.reviewedBy = Objects.requireNonNull(admin, "admin reviewer must not be null");
        this.status = RefundStatus.REJECTED;
        this.processedAt = Instant.now();
    }

    public void markProcessing() {
        this.status = RefundStatus.PROCESSING;
    }

    public void markProcessed(String providerRefundId) {
        this.providerRefundId = providerRefundId;
        this.status = RefundStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = RefundStatus.FAILED;
        this.processedAt = Instant.now();
    }
}
