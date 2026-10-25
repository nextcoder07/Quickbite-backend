package com.quickbite.shop;

import com.quickbite.common.exception.BadRequestException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {}

    public Product(Shop shop, String name, String description, BigDecimal price, int stockQuantity, String imagePath) {
        this.shop = Objects.requireNonNull(shop, "shop must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.description = description;
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be non-negative");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Product stock must be non-negative");
        }
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imagePath = imagePath;
        this.available = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Shop getShop() {
        return shop;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public String getImagePath() {
        return imagePath;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(String name, String description, BigDecimal price, Integer stockQuantity, String imagePath, Boolean available) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        if (description != null) this.description = description.trim();
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Product price must be non-negative");
            }
            this.price = price;
        }
        if (stockQuantity != null) {
            if (stockQuantity < 0) {
                throw new BadRequestException("Stock quantity must be non-negative");
            }
            this.stockQuantity = stockQuantity;
        }
        if (imagePath != null) this.imagePath = imagePath.trim();
        if (available != null) this.available = available;
        this.updatedAt = Instant.now();
    }

    public void decrementStock(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity to decrement must be positive");
        }
        if (this.stockQuantity < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + this.name);
        }
        this.stockQuantity -= quantity;
        if (this.stockQuantity == 0) {
            this.available = false;
        }
        this.updatedAt = Instant.now();
    }

    public void restoreStock(int quantity) {
        if (quantity > 0) {
            this.stockQuantity += quantity;
            this.available = true;
            this.updatedAt = Instant.now();
        }
    }
}
