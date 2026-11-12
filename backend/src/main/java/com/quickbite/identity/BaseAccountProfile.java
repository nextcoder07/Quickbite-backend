package com.quickbite.identity;

import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.model.BaseAuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Abstract OOP base class for all QuickBite account profile entities
 * (User, ShopOwner, DeliveryPartner, Admin).
 * Encapsulates shared identity linkage, status transitions, and polymorphic platform eligibility.
 */
@MappedSuperclass
public abstract class BaseAccountProfile extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_id", unique = true, nullable = false)
    private AccountIdentity identity;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected BaseAccountProfile() {
        super();
    }

    protected BaseAccountProfile(AccountIdentity identity, String displayName) {
        super();
        this.identity = Objects.requireNonNull(identity, "identity must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        this.accountStatus = AccountStatus.ACTIVE;
    }

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
    }

    public Long getId() {
        return id;
    }

    public AccountIdentity getIdentity() {
        return identity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void updateDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
            touchUpdatedAt();
        }
    }

    public void deactivate() {
        this.accountStatus = AccountStatus.DEACTIVATED;
        this.deactivatedAt = Instant.now();
        touchUpdatedAt();
    }

    public void suspend() {
        this.accountStatus = AccountStatus.SUSPENDED;
        touchUpdatedAt();
    }

    public void reactivate() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.deactivatedAt = null;
        touchUpdatedAt();
    }

    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }

    /**
     * Polymorphic account role snapshot implemented by each concrete profile subtype.
     */
    public abstract AccountTypeSnapshot getAccountType();

    /**
     * Polymorphic verification state (Buyers/Admins default to APPROVED;
     * Delivery Partners require explicit Admin verification).
     */
    public abstract VerificationStatus getVerificationStatus();

    /**
     * Returns true when this profile is permitted to perform operational platform actions.
     */
    public abstract boolean canOperateOnPlatform();
}

