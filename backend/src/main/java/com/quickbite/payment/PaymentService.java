package com.quickbite.payment;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.enums.PaymentStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.payment.dto.*;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final CustomerOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final AccountIdentityRepository identityRepository;
    private final RazorpayGateway razorpayGateway;
    private final AuditService auditService;

    public PaymentService(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            CustomerOrderRepository orderRepository,
            UserRepository userRepository,
            AdminRepository adminRepository,
            AccountIdentityRepository identityRepository,
            RazorpayGateway razorpayGateway,
            AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.identityRepository = identityRepository;
        this.razorpayGateway = razorpayGateway;
        this.auditService = auditService;
    }

    @Transactional
    public PaymentResponse createPaymentOrder(CurrentAccountPrincipal principal, CreatePaymentRequest request) {
        CustomerOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

        if (principal == null || !order.getBuyer().getIdentity().getId().equals(principal.getIdentityId())) {
            throw new ForbiddenException("Only the order buyer can initiate payment");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            throw new BadRequestException("Cannot pay for cancelled or rejected order");
        }

        // Check if there is already a captured payment for this order
        List<Payment> existingPayments = paymentRepository.findByOrderId(order.getId());
        for (Payment p : existingPayments) {
            if (p.isCaptured()) {
                throw new ConflictException("Payment for this order is already captured");
            }
        }

        PaymentGateway.GatewayOrder gwOrder = razorpayGateway.createOrder(
                order.getTotalAmount(), "INR", order.getOrderNumber()
        );

        Payment payment = new Payment(
                order,
                "RAZORPAY",
                gwOrder.providerOrderId(),
                gwOrder.amount(),
                gwOrder.currency(),
                request.idempotencyKey()
        );

        Payment saved = paymentRepository.save(payment);

        auditService.record(order.getBuyer().getIdentity(), "PAYMENT_INITIATED", "PAYMENT", String.valueOf(saved.getId()),
                Map.of("providerOrderId", gwOrder.providerOrderId(), "amount", gwOrder.amount().toString()));

        return PaymentResponse.from(saved, razorpayGateway.getKeyId());
    }

    @Transactional
    public PaymentResponse verifyPayment(CurrentAccountPrincipal principal, VerifyPaymentRequest request) {
        CustomerOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

        Payment payment = paymentRepository.findByProviderOrderId(request.providerOrderId())
                .orElseGet(() -> paymentRepository.findByOrderId(order.getId()).stream()
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for provider order")));

        if (payment.isCaptured()) {
            return PaymentResponse.from(payment, razorpayGateway.getKeyId());
        }

        boolean valid = razorpayGateway.verifyPaymentSignature(
                request.providerOrderId(), request.providerPaymentId(), request.signature()
        );

        if (!valid) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed: Invalid signature");
        }

        payment.markCaptured(request.providerPaymentId());
        Payment saved = paymentRepository.save(payment);

        auditService.record(order.getBuyer().getIdentity(), "PAYMENT_CAPTURED", "PAYMENT", String.valueOf(saved.getId()),
                Map.of("providerPaymentId", request.providerPaymentId(), "amount", saved.getAmount().toString()));

        return PaymentResponse.from(saved, razorpayGateway.getKeyId());
    }

    @Transactional
    public RefundResponse requestRefund(CurrentAccountPrincipal principal, CreateRefundRequest request) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + request.paymentId()));

        if (!payment.isCaptured()) {
            throw new BadRequestException("Only captured payments can be refunded");
        }

        AccountIdentity requester = identityRepository.findById(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Account identity not found"));

        // Enforce refund amount cap
        BigDecimal totalRefunded = refundRepository.sumEligibleRefundAmountByPaymentId(payment.getId());
        BigDecimal eligibleAmount = payment.getAmount().subtract(totalRefunded);

        if (request.amount().compareTo(eligibleAmount) > 0) {
            throw new BadRequestException("Refund amount " + request.amount() + " exceeds remaining eligible balance of " + eligibleAmount);
        }

        Refund refund = new Refund(payment, requester, request.amount(), request.reason(), request.idempotencyKey());
        Refund saved = refundRepository.save(refund);

        auditService.record(requester, "REFUND_REQUESTED", "REFUND", String.valueOf(saved.getId()),
                Map.of("paymentId", payment.getId(), "amount", request.amount().toString()));

        return RefundResponse.from(saved);
    }

    @Transactional
    public RefundResponse reviewRefund(CurrentAccountPrincipal principal, Long refundId, boolean approve, String note) {
        Admin admin = getAdmin(principal);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with id: " + refundId));

        if (approve) {
            refund.approve(admin);
            Payment payment = refund.getPayment();
            PaymentGateway.GatewayRefund gwRefund = razorpayGateway.processRefund(
                    payment.getProviderPaymentId() != null ? payment.getProviderPaymentId() : "pay_test_default",
                    refund.getAmount(),
                    refund.getReason()
            );

            if (gwRefund.success()) {
                refund.markProcessed(gwRefund.providerRefundId());
                payment.markRefunded();
                paymentRepository.save(payment);
            } else {
                refund.markFailed();
            }
        } else {
            refund.reject(admin);
        }

        Refund saved = refundRepository.save(refund);

        auditService.record(admin.getIdentity(), "REFUND_REVIEWED", "REFUND", String.valueOf(saved.getId()),
                Map.of("approved", approve, "reason", note != null ? note : ""));

        return RefundResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefunds() {
        return refundRepository.findAll().stream()
                .map(RefundResponse::from)
                .toList();
    }

    private Admin getAdmin(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return adminRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only admins can perform refund review"));
    }
}
