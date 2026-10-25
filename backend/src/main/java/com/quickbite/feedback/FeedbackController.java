package com.quickbite.feedback;

import com.quickbite.feedback.dto.CreateFeedbackRequest;
import com.quickbite.feedback.dto.FeedbackResponse;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "Customer Feedback", description = "Order ratings and reviews")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Submit order feedback", description = "Allows a buyer to rate and review their delivered order")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @AuthenticationPrincipal CurrentAccountPrincipal principal,
            @Valid @RequestBody CreateFeedbackRequest request) {
        return new ResponseEntity<>(feedbackService.submitFeedback(principal, request), HttpStatus.CREATED);
    }

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "Get feedback for shop", description = "Lists reviews submitted for a campus shop")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackForShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(feedbackService.getFeedbackForShop(shopId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Get my feedback", description = "Lists all feedback submitted by the current buyer")
    public ResponseEntity<List<FeedbackResponse>> getMyFeedback(
            @AuthenticationPrincipal CurrentAccountPrincipal principal) {
        return ResponseEntity.ok(feedbackService.getMyFeedback(principal));
    }
}
