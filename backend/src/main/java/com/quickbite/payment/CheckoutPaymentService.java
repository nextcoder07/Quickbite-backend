package com.quickbite.payment;

import com.quickbite.audit.AuditService;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.OrderService;
import com.quickbite.ordering.PricingService;
import com.quickbite.ordering.dto.CheckoutRequest;
import com.quickbite.ordering.dto.OrderResponse;
import com.quickbite.payment.dto.CompleteCheckoutPaymentRequest;
import com.quickbite.payment.dto.InitiateCheckoutPaymentResponse;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CheckoutPaymentService {

    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final RazorpayGateway razorpayGateway;
    private final AuditService auditService;

    public CheckoutPaymentService(
            OrderService orderService,
            PaymentRepository paymentRepository,
            RazorpayGateway razorpayGateway,
            AuditService auditService) {
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
        this.razorpayGateway = razorpayGateway;
        this.auditService = auditService;
    }

    /**
     * Validates the cart and initiates a gateway payment order without creating any CustomerOrder yet.
     */
    @Transactional(readOnly = true)
    public InitiateCheckoutPaymentResponse initiateCheckout(CurrentAccountPrincipal principal, CheckoutRequest request) {
        PricingService.PricingCalculation pricing = orderService.previewCheckout(principal, request);

        PaymentGateway.GatewayOrder gwOrder = razorpayGateway.createOrder(
                pricing.totalAmount(), "INR", "CHK-" + System.currentTimeMillis()
        );

        return new InitiateCheckoutPaymentResponse(
                gwOrder.providerOrderId(),
                pricing.totalAmount(),
                gwOrder.currency(),
                razorpayGateway.getKeyId(),
                pricing.subtotal(),
                pricing.deliveryFee(),
                pricing.discountAmount(),
                pricing.totalAmount()
        );
    }

    /**
     * Verifies payment signature first, then atomically creates the CustomerOrder and captured Payment.
     */
    @Transactional
    public OrderResponse completeCheckout(CurrentAccountPrincipal principal, CompleteCheckoutPaymentRequest request) {
        Optional<OrderResponse> existing = findExistingCapturedOrder(
                principal, request.providerOrderId(), request.providerPaymentId()
        );
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean valid = razorpayGateway.verifyPaymentSignature(
                request.providerOrderId(), request.providerPaymentId(), request.signature()
        );
        if (!valid) {
            throw new BadRequestException("Payment verification failed: Invalid signature");
        }

        CustomerOrder savedOrder = orderService.createPlacedOrder(principal, request.checkout());

        Payment payment = new Payment(
                savedOrder,
                "RAZORPAY",
                request.providerOrderId(),
                savedOrder.getTotalAmount(),
                "INR",
                request.providerOrderId()
        );
        payment.markCaptured(request.providerPaymentId());
        Payment savedPayment = paymentRepository.save(payment);

        auditService.record(
                savedOrder.getBuyer().getIdentity(),
                "PAYMENT_CAPTURED",
                "PAYMENT",
                String.valueOf(savedPayment.getId()),
                Map.of(
                        "providerPaymentId", request.providerPaymentId(),
                        "providerOrderId", request.providerOrderId(),
                        "amount", savedPayment.getAmount().toString()
                )
        );

        return OrderResponse.forBuyer(savedOrder);
    }

    private Optional<OrderResponse> findExistingCapturedOrder(
            CurrentAccountPrincipal principal, String providerOrderId, String providerPaymentId) {
        Optional<Payment> match = paymentRepository.findByProviderPaymentId(providerPaymentId)
                .or(() -> paymentRepository.findByProviderOrderId(providerOrderId));

        if (match.isEmpty() || !match.get().isCaptured()) {
            return Optional.empty();
        }

        Payment existing = match.get();
        if (principal == null || !existing.getOrder().getBuyer().getIdentity().getId().equals(principal.getIdentityId())) {
            throw new ForbiddenException("Payment reference belongs to a different account");
        }
        return Optional.of(OrderResponse.forBuyer(existing.getOrder()));
    }
}
