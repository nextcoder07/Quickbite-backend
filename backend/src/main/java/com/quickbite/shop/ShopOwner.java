package com.quickbite.shop;

import com.quickbite.common.enums.AccountStatus;
import com.quickbite.identity.AccountIdentity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "shop_owners")
public class ShopOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_id", unique = true, nullable = false)
    private AccountIdentity identity;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected ShopOwner() {}

    public ShopOwner(AccountIdentity identity, String displayName, String phone) {
        this.identity = Objects.requireNonNull(identity, "identity must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        this.phone = phone != null ? phone.trim() : null;
        this.accountStatus = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (accountStatus == null) accountStatus = AccountStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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

    public String getPhone() {
        return phone;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void updateProfile(String displayName, String phone) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }
        if (phone != null) {
            this.phone = phone.trim();
        }
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.accountStatus = AccountStatus.DEACTIVATED;
        this.deactivatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.accountStatus = AccountStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.deactivatedAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }
}
