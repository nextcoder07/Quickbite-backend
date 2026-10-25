package com.quickbite.feedback;

import com.quickbite.audit.AuditService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.exception.BadRequestException;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.feedback.dto.CreateFeedbackRequest;
import com.quickbite.feedback.dto.FeedbackResponse;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CustomerOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            CustomerOrderRepository orderRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.feedbackRepository = feedbackRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public FeedbackResponse submitFeedback(CurrentAccountPrincipal principal, CreateFeedbackRequest request) {
        User buyer = getBuyer(principal);

        CustomerOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenException("You can only submit feedback for your own orders");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Feedback can only be submitted after the order has been delivered");
        }

        if (feedbackRepository.existsByOrderId(order.getId())) {
            throw new ConflictException("Feedback has already been submitted for this order");
        }

        Feedback feedback = new Feedback(order, buyer, request.rating(), request.comment());
        Feedback saved = feedbackRepository.save(feedback);

        auditService.record(buyer.getIdentity(), "FEEDBACK_SUBMITTED", "FEEDBACK", String.valueOf(saved.getId()),
                Map.of("orderId", order.getId(), "rating", request.rating()));

        return FeedbackResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackForShop(Long shopId) {
        return feedbackRepository.findByOrderShopId(shopId).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMyFeedback(CurrentAccountPrincipal principal) {
        User buyer = getBuyer(principal);
        return feedbackRepository.findByBuyerId(buyer.getId()).stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    private User getBuyer(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ForbiddenException("Only buyers can submit feedback"));
    }
}
