package com.quickbite.shop;

import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.enums.ShopStatus;
import com.quickbite.feedback.Feedback;
import com.quickbite.feedback.FeedbackRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.ordering.OrderItem;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.dto.RecommendationFeedResponse;
import com.quickbite.shop.dto.RecommendedProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Hybrid Food Item Recommendation Service (OOP Strategy).
 * Ranks available food items using:
 * 1. Top Bayesian-weighted ratings & global order popularity
 * 2. Buyer profile signals (saved foodInterests + delivery location/GPS proximity)
 * 3. Buyer historical purchase affinity (past ordered products, shops & flavor keywords)
 * 4. Cold-Start Algorithm for fresh users (zero orders & no profile interests):
 *    Bayesian rating quality + popularity log boost + open-shop readiness + location proximity
 *    + shop-diversity interleaving so top results showcase multiple kitchens.
 */
@Service
public class RecommendationService {

    private static final double PRIOR_RATING = 4.2;
    private static final double PRIOR_WEIGHT = 2.0;
    private static final Set<String> STOP_WORDS = Set.of(
            "with", "and", "the", "for", "from", "fresh", "served", "hot", "style", "house", "special"
    );

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;

    public RecommendationService(
            ShopRepository shopRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CustomerOrderRepository orderRepository,
            FeedbackRepository feedbackRepository) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public RecommendationFeedResponse getRecommendedProducts(
            CurrentAccountPrincipal principal,
            String query,
            String locationFilter) {

        List<Shop> approvedShops = shopRepository.findByApprovalStatus(ShopStatus.APPROVED).stream()
                .filter(s -> !s.isDeactivated())
                .toList();

        GlobalMarketSignals marketSignals = buildGlobalMarketSignals();
        BuyerTasteProfile buyerProfile = buildBuyerTasteProfile(principal);

        boolean hasPersonalizationSignals = buyerProfile.hasHistoryOrPreferences();
        boolean coldStart = !hasPersonalizationSignals;

        String q = query != null ? query.trim().toLowerCase() : "";
        String loc = locationFilter != null ? locationFilter.trim().toLowerCase() : "";

        List<ScoredProductCandidate> candidates = new ArrayList<>();
        for (Shop shop : approvedShops) {
            if (!loc.isEmpty() && !"all".equals(loc)
                    && (shop.getLocation() == null || !shop.getLocation().toLowerCase().contains(loc))) {
                continue;
            }
            List<Product> products = productRepository.findByShopIdAndAvailableTrue(shop.getId());
            ShopRatingStats ratingStats = marketSignals.shopRatings().getOrDefault(shop.getId(), ShopRatingStats.EMPTY);

            for (Product product : products) {
                if (product.getStockQuantity() <= 0) {
                    continue;
                }
                if (!q.isEmpty() && !matchesQuery(product, shop, q)) {
                    continue;
                }
                candidates.add(scoreProduct(product, shop, ratingStats, marketSignals, buyerProfile, coldStart));
            }
        }

        List<ScoredProductCandidate> ranked = coldStart
                ? diversifyTopColdStartCandidates(candidates)
                : candidates.stream()
                        .sorted(Comparator.comparingDouble(ScoredProductCandidate::score).reversed()
                                .thenComparing(c -> c.product().getId()))
                        .toList();

        List<RecommendedProductResponse> items = ranked.stream()
                .map(c -> toResponse(c, coldStart))
                .toList();

        String summary = coldStart
                ? "Cold-Start Top Picks: Ranked by verified ratings, trending demand, location readiness & kitchen diversity"
                : "Personalized for You: Ranked by top ratings, your profile interests & past orders";

        return new RecommendationFeedResponse(
                hasPersonalizationSignals,
                coldStart,
                summary,
                buyerProfile.displayInterests(),
                items
        );
    }

    private ScoredProductCandidate scoreProduct(
            Product product,
            Shop shop,
            ShopRatingStats ratingStats,
            GlobalMarketSignals marketSignals,
            BuyerTasteProfile buyer,
            boolean coldStart) {

        int popularity = marketSignals.productOrderCounts().getOrDefault(product.getId(), 0);
        int productFeedbackCount = marketSignals.productRatingCounts().getOrDefault(product.getId(), 0);
        double productAvgRating = productFeedbackCount > 0
                ? marketSignals.productRatingSums().getOrDefault(product.getId(), 0.0) / productFeedbackCount
                : ratingStats.displayAverage();

        // Bayesian rating smooths small sample sizes toward PRIOR_RATING
        double totalWeightCount = productFeedbackCount > 0 ? productFeedbackCount : ratingStats.count();
        double rawSum = productFeedbackCount > 0
                ? marketSignals.productRatingSums().getOrDefault(product.getId(), 0.0)
                : ratingStats.sum();
        double bayesianRating = (rawSum + PRIOR_RATING * PRIOR_WEIGHT) / (totalWeightCount + PRIOR_WEIGHT);

        double score = bayesianRating * 10.0; // up to ~50 pts
        if (ratingStats.count() > 0) {
            score += Math.min(6.0, ratingStats.count() * 1.5);
        }
        score += Math.log1p(popularity) * 6.0;
        score += shop.isOpen() ? 14.0 : -18.0;

        double locationBoost = computeLocationBoost(buyer, shop);
        score += locationBoost;

        List<String> matchedProfileKeywords = findKeywordMatches(buyer.profileKeywords(), product, shop);
        List<String> matchedHistoryKeywords = findKeywordMatches(buyer.historyKeywords(), product, shop);
        int priorOrderQty = buyer.orderedProductCounts().getOrDefault(product.getId(), 0);
        int priorShopOrders = buyer.orderedShopCounts().getOrDefault(shop.getId(), 0);

        if (!coldStart) {
            score += Math.min(34.0, matchedProfileKeywords.size() * 20.0);
            if (priorOrderQty > 0) {
                score += Math.min(26.0, 14.0 + priorOrderQty * 4.0);
            }
            if (priorShopOrders > 0) {
                score += Math.min(10.0, priorShopOrders * 5.0);
            }
            score += Math.min(20.0, matchedHistoryKeywords.size() * 12.0);
        }

        String reason = buildReason(
                coldStart,
                shop,
                productAvgRating,
                (int) totalWeightCount,
                popularity,
                locationBoost,
                matchedProfileKeywords,
                priorOrderQty,
                matchedHistoryKeywords
        );

        return new ScoredProductCandidate(
                product,
                shop,
                roundToOneDecimal(productAvgRating),
                (int) totalWeightCount,
                popularity,
                Math.round(score * 100.0) / 100.0,
                reason
        );
    }

    /**
     * Cold-start anti-monopoly interleaver: applies a slight diversity rotation so fresh users
     * see top-rated choices across multiple kitchens instead of every slot from one shop.
     */
    private List<ScoredProductCandidate> diversifyTopColdStartCandidates(List<ScoredProductCandidate> candidates) {
        List<ScoredProductCandidate> remaining = new ArrayList<>(candidates);
        remaining.sort(Comparator.comparingDouble(ScoredProductCandidate::score).reversed()
                .thenComparing(c -> c.product().getId()));

        List<ScoredProductCandidate> diversified = new ArrayList<>();
        Map<Long, Integer> shopCountSeen = new HashMap<>();

        while (!remaining.isEmpty()) {
            int bestIndex = 0;
            double bestAdjustedScore = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < remaining.size(); i++) {
                ScoredProductCandidate c = remaining.get(i);
                int seen = shopCountSeen.getOrDefault(c.shop().getId(), 0);
                double adjusted = c.score() - (seen * 7.5);
                if (adjusted > bestAdjustedScore) {
                    bestAdjustedScore = adjusted;
                    bestIndex = i;
                }
            }
            ScoredProductCandidate chosen = remaining.remove(bestIndex);
            shopCountSeen.merge(chosen.shop().getId(), 1, Integer::sum);
            diversified.add(chosen);
        }
        return diversified;
    }

    private GlobalMarketSignals buildGlobalMarketSignals() {
        Map<Long, Double> shopSum = new HashMap<>();
        Map<Long, Integer> shopCount = new HashMap<>();
        Map<Long, Double> productSum = new HashMap<>();
        Map<Long, Integer> productCount = new HashMap<>();

        for (Feedback fb : feedbackRepository.findAll()) {
            CustomerOrder order = fb.getOrder();
            if (order == null || order.getShop() == null) continue;
            Long shopId = order.getShop().getId();
            shopSum.merge(shopId, (double) fb.getRating(), Double::sum);
            shopCount.merge(shopId, 1, Integer::sum);

            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    Long prodId = item.getProduct().getId();
                    productSum.merge(prodId, (double) fb.getRating(), Double::sum);
                    productCount.merge(prodId, 1, Integer::sum);
                }
            }
        }

        Map<Long, ShopRatingStats> shopRatings = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : shopCount.entrySet()) {
            Long shopId = entry.getKey();
            shopRatings.put(shopId, new ShopRatingStats(shopSum.getOrDefault(shopId, 0.0), entry.getValue()));
        }

        Map<Long, Integer> productOrderCounts = new HashMap<>();
        for (CustomerOrder order : orderRepository.findAll()) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    productOrderCounts.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
                }
            }
        }

        return new GlobalMarketSignals(shopRatings, productSum, productCount, productOrderCounts);
    }

    private BuyerTasteProfile buildBuyerTasteProfile(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null
                || principal.getAccountType() != AccountTypeSnapshot.BUYER) {
            return BuyerTasteProfile.EMPTY;
        }

        Optional<User> userOpt = userRepository.findByIdentityId(principal.getIdentityId());
        if (userOpt.isEmpty()) {
            return BuyerTasteProfile.EMPTY;
        }

        User buyer = userOpt.get();
        List<String> displayInterests = parseInterestTags(buyer.getFoodInterests());
        Set<String> profileKeywords = new LinkedHashSet<>();
        for (String tag : displayInterests) {
            profileKeywords.addAll(tokenize(tag));
        }

        Map<Long, Integer> orderedProducts = new HashMap<>();
        Map<Long, Integer> orderedShops = new HashMap<>();
        Set<String> historyKeywords = new LinkedHashSet<>();

        List<CustomerOrder> pastOrders = orderRepository.findByBuyerIdOrderByPlacedAtDesc(buyer.getId());
        for (CustomerOrder order : pastOrders) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
                continue;
            }
            if (order.getShop() != null) {
                orderedShops.merge(order.getShop().getId(), 1, Integer::sum);
            }
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    orderedProducts.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
                    historyKeywords.addAll(tokenize(item.getProduct().getName()));
                    historyKeywords.addAll(tokenize(item.getProduct().getDescription()));
                } else {
                    historyKeywords.addAll(tokenize(item.getProductNameSnapshot()));
                }
            }
        }

        return new BuyerTasteProfile(
                buyer.getDefaultAddress(),
                buyer.getDefaultLatitude(),
                buyer.getDefaultLongitude(),
                displayInterests,
                profileKeywords,
                historyKeywords,
                orderedProducts,
                orderedShops
        );
    }

    private double computeLocationBoost(BuyerTasteProfile buyer, Shop shop) {
        if (buyer.latitude() != null && buyer.longitude() != null
                && shop.getLatitude() != null && shop.getLongitude() != null) {
            double km = haversineKm(buyer.latitude(), buyer.longitude(), shop.getLatitude(), shop.getLongitude());
            if (km <= 3.0) return 12.0;
            if (km <= 8.0) return 8.0;
            if (km <= 15.0) return 4.0;
            return 0.0;
        }
        if (buyer.defaultAddress() != null && !buyer.defaultAddress().isBlank() && shop.getLocation() != null) {
            String shopLoc = shop.getLocation().toLowerCase();
            for (String token : tokenize(buyer.defaultAddress())) {
                if (token.length() >= 4 && shopLoc.contains(token)) {
                    return 9.0;
                }
            }
        }
        return 0.0;
    }

    private List<String> findKeywordMatches(Set<String> keywords, Product product, Shop shop) {
        if (keywords.isEmpty()) return List.of();
        String haystack = (product.getName() + " "
                + (product.getDescription() != null ? product.getDescription() : "") + " "
                + (shop.getDescription() != null ? shop.getDescription() : "")).toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String kw : keywords) {
            if (haystack.contains(kw)) {
                matches.add(kw);
            }
        }
        return matches;
    }

    private String buildReason(
            boolean coldStart,
            Shop shop,
            double avgRating,
            int reviewCount,
            int popularity,
            double locationBoost,
            List<String> profileMatches,
            int priorOrderQty,
            List<String> historyMatches) {

        List<String> parts = new ArrayList<>();
        if (!coldStart) {
            if (priorOrderQty > 0) {
                parts.add("Ordered Before");
            }
            if (!profileMatches.isEmpty()) {
                parts.add("Matches Profile: " + capitalize(profileMatches.get(0)));
            } else if (!historyMatches.isEmpty() && priorOrderQty == 0) {
                parts.add("Similar to Your Past Buys");
            }
        }
        if (reviewCount > 0 && avgRating >= 4.0) {
            parts.add("Top Rated (" + roundToOneDecimal(avgRating) + "★)");
        }
        if (popularity >= 2 && parts.size() < 2) {
            parts.add("Popular Pick");
        }
        if (locationBoost > 0 && parts.size() < 2) {
            parts.add("Near Your Area");
        }
        if (parts.isEmpty()) {
            parts.add(coldStart
                    ? (shop.isOpen() ? "Fresh Top-Rated Pick" : "Top Rated")
                    : (shop.isOpen() ? "Recommended For You" : "Saved Kitchen"));
        }
        return String.join(" • ", parts);
    }

    private boolean matchesQuery(Product p, Shop s, String q) {
        return (p.getName() != null && p.getName().toLowerCase().contains(q))
                || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                || (s.getName() != null && s.getName().toLowerCase().contains(q))
                || (s.getLocation() != null && s.getLocation().toLowerCase().contains(q));
    }

    private List<String> parseInterestTags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("[,;|]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.length() >= 3 && !STOP_WORDS.contains(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return "";
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private double roundToOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private RecommendedProductResponse toResponse(ScoredProductCandidate c, boolean coldStart) {
        Product p = c.product();
        Shop s = c.shop();
        return new RecommendedProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getImagePath(),
                s.getId(),
                s.getName(),
                s.getLocation(),
                s.getLatitude(),
                s.getLongitude(),
                s.isOpen(),
                c.avgRating(),
                c.ratingCount(),
                c.popularity(),
                c.score(),
                c.reason(),
                coldStart
        );
    }

    private record ShopRatingStats(double sum, int count) {
        static final ShopRatingStats EMPTY = new ShopRatingStats(0.0, 0);

        double displayAverage() {
            return count > 0 ? sum / count : 4.2;
        }
    }

    private record GlobalMarketSignals(
            Map<Long, ShopRatingStats> shopRatings,
            Map<Long, Double> productRatingSums,
            Map<Long, Integer> productRatingCounts,
            Map<Long, Integer> productOrderCounts
    ) {}

    private record BuyerTasteProfile(
            String defaultAddress,
            Double latitude,
            Double longitude,
            List<String> displayInterests,
            Set<String> profileKeywords,
            Set<String> historyKeywords,
            Map<Long, Integer> orderedProductCounts,
            Map<Long, Integer> orderedShopCounts
    ) {
        static final BuyerTasteProfile EMPTY = new BuyerTasteProfile(
                null, null, null, List.of(), Set.of(), Set.of(), Map.of(), Map.of());

        boolean hasHistoryOrPreferences() {
            return !profileKeywords.isEmpty() || !orderedProductCounts.isEmpty();
        }
    }

    private record ScoredProductCandidate(
            Product product,
            Shop shop,
            double avgRating,
            int ratingCount,
            int popularity,
            double score,
            String reason
    ) {}
}

