package com.quickbite.feedback;

import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.feedback.dto.CreateFeedbackRequest;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackEligibilityTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private CustomerOrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackRepository, orderRepository, userRepository, auditService);
    }

    @Test
    @DisplayName("Should reject feedback submission before order is delivered")
    void testFeedbackOnlyDelivered() {
        AccountIdentity identity = new AccountIdentity("uid", "buyer@campus.edu");
        User buyer = new User(identity, "Buyer", "123");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getBuyer()).thenReturn(buyer);
        when(order.getStatus()).thenReturn(OrderStatus.PREPARING);

        when(userRepository.findByIdentityId(1L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        CurrentAccountPrincipal principal = new CurrentAccountPrincipal(1L, "uid", "buyer@campus.edu", null, null, null);
        CreateFeedbackRequest request = new CreateFeedbackRequest(100L, 5, "Great food!");

        assertThrows(BadRequestException.class, () -> feedbackService.submitFeedback(principal, request));
    }

    @Test
    @DisplayName("Should reject duplicate feedback for the same order")
    void testDuplicateFeedbackRejection() {
        AccountIdentity identity = new AccountIdentity("uid", "buyer@campus.edu");
        User buyer = new User(identity, "Buyer", "123");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(100L);
        when(order.getBuyer()).thenReturn(buyer);
        when(order.getStatus()).thenReturn(OrderStatus.DELIVERED);

        when(userRepository.findByIdentityId(1L)).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(feedbackRepository.existsByOrderId(100L)).thenReturn(true);

        CurrentAccountPrincipal principal = new CurrentAccountPrincipal(1L, "uid", "buyer@campus.edu", null, null, null);
        CreateFeedbackRequest request = new CreateFeedbackRequest(100L, 5, "Second review attempt");

        assertThrows(ConflictException.class, () -> feedbackService.submitFeedback(principal, request));
    }
}
