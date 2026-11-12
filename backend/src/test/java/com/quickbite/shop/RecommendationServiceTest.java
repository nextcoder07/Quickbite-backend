package com.quickbite.shop;

import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.feedback.Feedback;
import com.quickbite.feedback.FeedbackRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.ordering.OrderItem;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.dto.RecommendationFeedResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationServiceTest {

    @Autowired private RecommendationService recommendationService;
    @Autowired private MarketplaceService marketplaceService;
    @Autowired private AccountIdentityRepository identityRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopOwnerRepository shopOwnerRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerOrderRepository orderRepository;
    @Autowired private FeedbackRepository feedbackRepository;

    private void seedSecondApprovedShopForTest() {
        User existingBuyer = userRepository.findAll().stream().findFirst().orElseThrow();
        AccountIdentity owner2Id = identityRepository.save(new AccountIdentity("uid-test-owner-2", "test.shop2@quickbite.com"));
        ShopOwner owner2 = shopOwnerRepository.save(new ShopOwner(owner2Id, "Elena Rossi", "9900112233"));
        Shop shop2 = new Shop(
                owner2,
                "Harbor Stone Pizzeria & Café",
                "Wood-fired artisan pizzas and pastas.",
                "18 Riverside Plaza, Koramangala, Bengaluru",
                null,
                12.9352,
                77.6245
        );
        shop2.approve();
        shop2.setOpen(true);
        Shop savedShop2 = shopRepository.save(shop2);
        Product pizza = productRepository.save(new Product(
                savedShop2,
                "Margherita Sourdough Pizza",
                "Hand-stretched fermented dough with San Marzano tomatoes and basil.",
                new BigDecimal("240.00"),
                18,
                null
        ));

        CustomerOrder order = new CustomerOrder(
                "QB-TEST-2001", existingBuyer, savedShop2,
                new BigDecimal("240.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Test order",
                existingBuyer.getDefaultAddress(), existingBuyer.getDefaultLatitude(), existingBuyer.getDefaultLongitude()
        );
        order.addItem(new OrderItem(pizza, 1));
        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.PREPARING);
        order.transitionTo(OrderStatus.READY);
        order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
        order.completeDelivery(order.getDeliveryOtpForBuyer());
        order = orderRepository.save(order);
        feedbackRepository.save(new Feedback(order, existingBuyer, 5, "Great sourdough pizza!"));
    }

    @Test
    void returningBuyerGetsPersonalizedRecommendationsByRatingsInterestsAndPastBuys() {
        var identity = identityRepository.findByEmailIgnoreCase("buyer@quickbite.com").orElseThrow();
        User buyer = userRepository.findByIdentityId(identity.getId()).orElseThrow();
        CurrentAccountPrincipal principal = new CurrentAccountPrincipal(
                identity.getId(), identity.getFirebaseUid(), identity.getEmail(),
                AccountTypeSnapshot.BUYER, buyer.getId(), AccountStatus.ACTIVE
        );

        RecommendationFeedResponse feed = recommendationService.getRecommendedProducts(principal, null, null);

        assertTrue(feed.personalized(), "Returning buyer with food interests & orders should get personalized ranking");
        assertFalse(feed.coldStart());
        assertFalse(feed.items().isEmpty());
        assertFalse(feed.activeInterests().isEmpty());

        String topItemName = feed.items().get(0).productName().toLowerCase();
        assertTrue(topItemName.contains("paneer") || topItemName.contains("biryani"),
                "Top personalized item should align with past buys & profile interests, got: " + topItemName);
    }

    @Test
    void freshBuyerReceivesColdStartTopRatedDiverseRecommendations() {
        seedSecondApprovedShopForTest();
        AccountIdentity freshIdentity = identityRepository.save(new AccountIdentity("uid-test-fresh", "fresh.buyer@quickbite.com"));
        User freshBuyer = userRepository.save(new User(freshIdentity, "Fresh Buyer", "9800000000"));
        CurrentAccountPrincipal principal = new CurrentAccountPrincipal(
                freshIdentity.getId(), freshIdentity.getFirebaseUid(), freshIdentity.getEmail(),
                AccountTypeSnapshot.BUYER, freshBuyer.getId(), AccountStatus.ACTIVE
        );

        RecommendationFeedResponse feed = recommendationService.getRecommendedProducts(principal, null, null);

        assertFalse(feed.personalized());
        assertTrue(feed.coldStart(), "Fresh buyer without orders or interests must trigger cold-start algorithm");
        assertTrue(feed.items().size() >= 4);
        assertTrue(feed.items().get(0).coldStartPick());

        List<Long> topTwoShopIds = feed.items().stream().limit(2).map(i -> i.shopId()).distinct().toList();
        assertEquals(2, topTwoShopIds.size(), "Cold-start diversity should showcase multiple top-rated kitchens early");
    }

    @Test
    void browsingAllShopsLoadsAllApprovedShopsBeforeSearchAndSupportsFilters() {
        seedSecondApprovedShopForTest();

        var allShops = marketplaceService.getAllApprovedShops();
        assertTrue(allShops.size() >= 2, "Dedicated shop browse page should load all approved shops upfront");

        var itemFiltered = marketplaceService.searchShops("Margherita", null, true);
        assertEquals(1, itemFiltered.size());
        assertEquals("Harbor Stone Pizzeria & Café", itemFiltered.get(0).name());

        var locationFiltered = marketplaceService.searchShops(null, "Indiranagar", true);
        assertTrue(locationFiltered.stream().anyMatch(s -> s.name().equals("Urban Spice Kitchen")));
    }
}
