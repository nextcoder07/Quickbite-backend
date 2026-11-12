package com.quickbite.shop.dto;

import java.util.List;

/**
 * Response envelope describing whether the ranking used personalized signals
 * (past purchases + buyer profile interests) or the cold-start algorithm for fresh buyers.
 */
public record RecommendationFeedResponse(
        boolean personalized,
        boolean coldStart,
        String strategySummary,
        List<String> activeInterests,
        List<RecommendedProductResponse> items
) {
}

