package com.quickbite.ordering;

import com.quickbite.buyer.User;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.InvalidStateTransitionException;
import com.quickbite.shop.Shop;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "buyer_note", columnDefinition = "TEXT")
    private String buyerNote;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerOrder() {}

    public CustomerOrder(String orderNumber, User buyer, Shop shop, BigDecimal subtotal,
                         BigDecimal deliveryFee, BigDecimal discountAmount, String buyerNote) {
        this.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber must not be null");
        this.buyer = Objects.requireNonNull(buyer, "buyer must not be null");
        this.shop = Objects.requireNonNull(shop, "shop must not be null");
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal must not be null");
        this.deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.totalAmount = this.subtotal.add(this.deliveryFee).subtract(this.discountAmount);
        this.buyerNote = buyerNote;
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (placedAt == null) placedAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (status == null) status = OrderStatus.PLACED;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public User getBuyer() {
        return buyer;
    }

    public Shop getShop() {
        return shop;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getBuyerNote() {
        return buyerNote;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!isValidTransition(this.status, newStatus)) {
            throw new InvalidStateTransitionException(
                    "Invalid order transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status != OrderStatus.PLACED && this.status != OrderStatus.ACCEPTED) {
            throw new InvalidStateTransitionException("Order in status " + this.status + " cannot be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason != null ? reason.trim() : "Cancelled by user";
        this.updatedAt = Instant.now();
    }

    public boolean canBeCancelled() {
        return this.status == OrderStatus.PLACED || this.status == OrderStatus.ACCEPTED;
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        if (current == next) return true;
        return switch (current) {
            case PLACED -> next == OrderStatus.ACCEPTED || next == OrderStatus.REJECTED || next == OrderStatus.CANCELLED;
            case ACCEPTED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.READY;
            case READY -> next == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> next == OrderStatus.DELIVERED;
            case DELIVERED, REJECTED, CANCELLED -> false;
        };
    }
}
