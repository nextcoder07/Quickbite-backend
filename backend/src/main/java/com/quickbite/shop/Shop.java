package com.quickbite.shop;

import com.quickbite.common.enums.ShopStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", unique = true, nullable = false)
    private ShopOwner owner;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "campus_location", nullable = false, length = 200)
    private String campusLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ShopStatus approvalStatus;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "is_deactivated", nullable = false)
    private boolean deactivated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected Shop() {}

    public Shop(ShopOwner owner, String name, String description, String campusLocation, String imagePath) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.description = description;
        this.campusLocation = Objects.requireNonNull(campusLocation, "campusLocation must not be null").trim();
        this.imagePath = imagePath;
        this.approvalStatus = ShopStatus.PENDING;
        this.open = false;
        this.deactivated = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (approvalStatus == null) approvalStatus = ShopStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ShopOwner getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getCampusLocation() {
        return campusLocation;
    }

    public ShopStatus getApprovalStatus() {
        return approvalStatus;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isDeactivated() {
        return deactivated;
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

    public void updateDetails(String name, String description, String campusLocation, String imagePath) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        if (description != null) this.description = description.trim();
        if (campusLocation != null && !campusLocation.isBlank()) this.campusLocation = campusLocation.trim();
        if (imagePath != null) this.imagePath = imagePath.trim();
        this.updatedAt = Instant.now();
    }

    public void setOpen(boolean open) {
        this.open = open;
        this.updatedAt = Instant.now();
    }

    public void approve() {
        this.approvalStatus = ShopStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        this.approvalStatus = ShopStatus.REJECTED;
        this.open = false;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.approvalStatus = ShopStatus.SUSPENDED;
        this.open = false;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.deactivated = true;
        this.open = false;
        this.deactivatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean canAcceptOrders() {
        return this.approvalStatus == ShopStatus.APPROVED && !this.deactivated && this.open;
    }
}
