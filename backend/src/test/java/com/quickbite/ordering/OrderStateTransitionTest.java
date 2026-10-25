package com.quickbite.ordering;

import com.quickbite.buyer.User;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.InvalidStateTransitionException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateTransitionTest {

    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        AccountIdentity identity = new AccountIdentity("uid-b", "b@campus.edu");
        User buyer = new User(identity, "Test Buyer", "123");
        ShopOwner owner = new ShopOwner(new AccountIdentity("uid-o", "o@campus.edu"), "Test Owner", "456");
        Shop shop = new Shop(owner, "Test Shop", "Desc", "Loc", null);

        order = new CustomerOrder("QB-101", buyer, shop, new BigDecimal("100.00"), new BigDecimal("30.00"), BigDecimal.ZERO, "Note");
    }

    @Test
    @DisplayName("Should progress through valid lifecycle successfully")
    void testValidLifecycle() {
        assertEquals(OrderStatus.PLACED, order.getStatus());

        order.transitionTo(OrderStatus.ACCEPTED);
        assertEquals(OrderStatus.ACCEPTED, order.getStatus());

        order.transitionTo(OrderStatus.PREPARING);
        assertEquals(OrderStatus.PREPARING, order.getStatus());

        order.transitionTo(OrderStatus.READY);
        assertEquals(OrderStatus.READY, order.getStatus());

        order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());

        order.transitionTo(OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    @DisplayName("Should reject invalid state transition skipping intermediate steps")
    void testInvalidTransition() {
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertThrows(InvalidStateTransitionException.class, () -> order.transitionTo(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Should allow cancellation from PLACED but reject cancellation once DELIVERED")
    void testCancellationEligibility() {
        assertTrue(order.canBeCancelled());
        order.transitionTo(OrderStatus.ACCEPTED);
        assertTrue(order.canBeCancelled());

        order.transitionTo(OrderStatus.PREPARING);
        assertFalse(order.canBeCancelled());
        assertThrows(InvalidStateTransitionException.class, () -> order.cancel("Too late"));
    }
}
