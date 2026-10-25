package com.quickbite.payment;

import com.quickbite.accountdeletion.AccountDeletionRequest;
import com.quickbite.administration.Admin;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.PaymentStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.payment.dto.CreateRefundRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCapAndStateTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private AccountIdentityRepository identityRepository;
    @Mock private RazorpayGateway razorpayGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository, refundRepository, null, null, null,
                identityRepository, razorpayGateway, mock(com.quickbite.audit.AuditService.class)
        );
    }

    @Test
    @DisplayName("Should prevent refund exceeding eligible remaining balance")
    void testRefundCapEnforcement() {
        CustomerOrder order = mock(CustomerOrder.class);
        Payment payment = new Payment(order, "RAZORPAY", "order_123", new BigDecimal("500.00"), "INR", null);
        payment.markCaptured("pay_123");
        ReflectionTestUtils.setField(payment, "id", 10L);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        AccountIdentity identity = new AccountIdentity("uid", "user@campus.edu");
        ReflectionTestUtils.setField(identity, "id", 1L);
        when(identityRepository.findById(1L)).thenReturn(Optional.of(identity));

        // Already refunded 350.00
        when(refundRepository.sumEligibleRefundAmountByPaymentId(10L)).thenReturn(new BigDecimal("350.00"));

        CurrentAccountPrincipal principal = new CurrentAccountPrincipal(1L, "uid", "user@campus.edu", null, null, null);

        // Requesting 200.00 (remaining is 150.00) -> should fail
        CreateRefundRequest request = new CreateRefundRequest(10L, new BigDecimal("200.00"), "Defective item", null);

        assertThrows(BadRequestException.class, () -> paymentService.requestRefund(principal, request));
    }

    @Test
    @DisplayName("Should prevent admin self-review on deletion requests")
    void testAdminSelfReviewProhibition() {
        AccountIdentity identity = new AccountIdentity("uid-admin", "admin@campus.edu");
        ReflectionTestUtils.setField(identity, "id", 5L);

        Admin admin = new Admin(identity, "Admin User");

        AccountDeletionRequest request = new AccountDeletionRequest(identity, AccountTypeSnapshot.SHOP_OWNER, "Leaving campus");

        assertThrows(ForbiddenException.class, () -> request.confirm(admin, "Approved self"));
    }
}
