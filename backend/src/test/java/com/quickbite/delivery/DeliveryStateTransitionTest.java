package com.quickbite.delivery;

import com.quickbite.buyer.User;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryStateTransitionTest {

    private DeliveryAssignment assignment;
    private DeliveryPartner partner1;
    private DeliveryPartner partner2;

    @BeforeEach
    void setUp() {
        CustomerOrder order = new CustomerOrder(
                "QB-202",
                new User(new AccountIdentity("b", "b@campus.edu"), "Buyer", "123"),
                new Shop(new ShopOwner(new AccountIdentity("o", "o@campus.edu"), "Owner", "456"), "Shop", "D", "L", null),
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, null
        );

        assignment = new DeliveryAssignment(order);

        partner1 = new DeliveryPartner(new AccountIdentity("p1", "p1@campus.edu"), "Partner 1", "111");
        ReflectionTestUtils.setField(partner1, "id", 1L);

        partner2 = new DeliveryPartner(new AccountIdentity("p2", "p2@campus.edu"), "Partner 2", "222");
        ReflectionTestUtils.setField(partner2, "id", 2L);
    }

    @Test
    @DisplayName("Should verify partner identity on status updates")
    void testPartnerAuthorizationCheck() {
        assignment.assignTo(partner1);
        assignment.accept(partner1);
        assignment.markPickedUp(partner1);

        // Partner 2 attempts to deliver Partner 1's assignment
        assertThrows(ForbiddenException.class, () -> assignment.markDelivered(partner2));

        // Partner 1 completes delivery
        assignment.markDelivered(partner1);
        assertEquals(DeliveryStatus.DELIVERED, assignment.getStatus());
    }
}
