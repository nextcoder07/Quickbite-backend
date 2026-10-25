package com.quickbite.ordering;

import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.delivery.DeliveryAssignment;
import com.quickbite.delivery.DeliveryAssignmentRepository;
import com.quickbite.ordering.dto.CheckoutRequest;
import com.quickbite.ordering.dto.OrderItemRequest;
import com.quickbite.ordering.dto.OrderResponse;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.Product;
import com.quickbite.shop.ProductRepository;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwnerRepository;
import com.quickbite.shop.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final PricingService pricingService;
    private final AuditService auditService;

    public OrderService(
            CustomerOrderRepository orderRepository,
            ProductRepository productRepository,
            ShopRepository shopRepository,
            UserRepository userRepository,
            ShopOwnerRepository shopOwnerRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            PricingService pricingService,
            AuditService auditService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.pricingService = pricingService;
        this.auditService = auditService;
    }

    @Transactional
    public OrderResponse checkout(CurrentAccountPrincipal principal, CheckoutRequest request) {
        User buyer = getBuyer(principal);
        if (!buyer.isActive()) {
            throw new ForbiddenException("Account is not active. Checkout is prohibited.");
        }

        Shop shop = shopRepository.findById(request.shopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + request.shopId()));

        if (!shop.canAcceptOrders()) {
            throw new BadRequestException("Shop is currently closed or not approved to accept orders");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        // Deduplicate product items and validate
        Map<Long, Integer> productQuantities = new HashMap<>();
        for (OrderItemRequest itemReq : request.items()) {
            productQuantities.merge(itemReq.productId(), itemReq.quantity(), Integer::sum);
        }

        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Product " + product.getName() + " does not belong to shop " + shop.getName());
            }

            if (!product.isAvailable() || product.getStockQuantity() < qty) {
                throw new BadRequestException("Product " + product.getName() + " is out of stock or has insufficient quantity");
            }

            // Atomically decrement stock
            product.decrementStock(qty);
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(product, product.getName(), product.getPrice(), qty);
            orderItems.add(orderItem);
        }

        PricingService.PricingCalculation pricing = pricingService.calculatePricing(orderItems);

        String orderNumber = "QB-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        CustomerOrder order = new CustomerOrder(
                orderNumber,
                buyer,
                shop,
                pricing.subtotal(),
                pricing.deliveryFee(),
                pricing.discountAmount(),
                request.buyerNote()
        );

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        CustomerOrder savedOrder = orderRepository.save(order);

        // Create default unassigned delivery assignment
        DeliveryAssignment assignment = new DeliveryAssignment(savedOrder);
        deliveryAssignmentRepository.save(assignment);

        auditService.record(buyer.getIdentity(), "ORDER_PLACED", "ORDER", savedOrder.getOrderNumber(),
                Map.of("totalAmount", savedOrder.getTotalAmount().toString(), "shopId", shop.getId()));

        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(CurrentAccountPrincipal principal, Long orderId, OrderStatus newStatus) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOrderTransitionActor(principal, order, newStatus);

        order.transitionTo(newStatus);

        // If order was rejected or cancelled, restore product stock
        if (newStatus == OrderStatus.REJECTED || newStatus == OrderStatus.CANCELLED) {
            restoreStockForOrder(order);
            deliveryAssignmentRepository.findByOrderId(order.getId()).ifPresent(da -> {
                if (da.getStatus() == DeliveryStatus.UNASSIGNED || da.getStatus() == DeliveryStatus.ASSIGNED) {
                    da.reportFailure(null, "Order cancelled or rejected by shop");
                }
            });
        }

        CustomerOrder updated = orderRepository.save(order);
        auditService.record(null, "ORDER_STATUS_CHANGED", "ORDER", order.getOrderNumber(),
                Map.of("newStatus", newStatus.name()));

        return OrderResponse.from(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(CurrentAccountPrincipal principal, Long orderId, String reason) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean isBuyer = principal != null && order.getBuyer().getIdentity().getId().equals(principal.getIdentityId());
        boolean isShopOwner = principal != null && order.getShop().getOwner().getIdentity().getId().equals(principal.getIdentityId());

        if (!isBuyer && !isShopOwner) {
            throw new ForbiddenException("Only the ordering buyer or shop owner can cancel this order");
        }

        order.cancel(reason);
        restoreStockForOrder(order);

        deliveryAssignmentRepository.findByOrderId(order.getId()).ifPresent(da -> {
            if (da.getStatus() != DeliveryStatus.DELIVERED) {
                da.reportFailure(null, "Order cancelled: " + reason);
            }
        });

        CustomerOrder saved = orderRepository.save(order);
        auditService.record(null, "ORDER_CANCELLED", "ORDER", order.getOrderNumber(),
                Map.of("reason", reason != null ? reason : ""));

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getBuyerOrders(CurrentAccountPrincipal principal) {
        User buyer = getBuyer(principal);
        return orderRepository.findByBuyerIdOrderByPlacedAtDesc(buyer.getId())
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getShopOrders(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        var shopOwner = shopOwnerRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only shop owners can view shop orders"));
        Shop shop = shopRepository.findByOwnerId(shopOwner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No shop found for owner"));

        return orderRepository.findByShopIdOrderByPlacedAtDesc(shop.getId())
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(CurrentAccountPrincipal principal, Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.from(order);
    }

    private void restoreStockForOrder(CustomerOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.restoreStock(item.getQuantity());
            productRepository.save(product);
        }
    }

    private void validateOrderTransitionActor(CurrentAccountPrincipal principal, CustomerOrder order, OrderStatus newStatus) {
        if (principal == null || principal.getAccountType() == null) {
            throw new ForbiddenException("Authentication required");
        }

        switch (newStatus) {
            case ACCEPTED, REJECTED, PREPARING, READY -> {
                if (!order.getShop().getOwner().getIdentity().getId().equals(principal.getIdentityId())) {
                    throw new ForbiddenException("Only the shop owner can update preparation and acceptance status");
                }
            }
            case OUT_FOR_DELIVERY, DELIVERED -> {
                // Must be delivery partner assigned to this order
                DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(order.getId())
                        .orElseThrow(() -> new BadRequestException("No delivery assignment found for this order"));
                if (assignment.getDeliveryPartner() == null ||
                        !assignment.getDeliveryPartner().getIdentity().getId().equals(principal.getIdentityId())) {
                    throw new ForbiddenException("Only the assigned delivery partner can update transit status");
                }
            }
            default -> {}
        }
    }

    private User getBuyer(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only buyers can perform checkout"));
    }
}
