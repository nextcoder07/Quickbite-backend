package com.quickbite.config;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.feedback.Feedback;
import com.quickbite.feedback.FeedbackRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.ordering.OrderItem;
import com.quickbite.shop.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AccountIdentityRepository identityRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository orderRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            AccountIdentityRepository identityRepository,
            AdminRepository adminRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            ShopRepository shopRepository,
            ProductRepository productRepository,
            CustomerOrderRepository orderRepository,
            FeedbackRepository feedbackRepository,
            PasswordEncoder passwordEncoder) {
        this.identityRepository = identityRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminRepository.count() > 0 || userRepository.count() > 0
                || shopOwnerRepository.count() > 0 || deliveryPartnerRepository.count() > 0) {
            log.info("Database already initialized. Skipping starter seed.");
            return;
        }

        log.info("Seeding initial starter accounts (1 per role)...");
        String starterHash = passwordEncoder.encode("QuickBite@123");

        // 1. Starter Platform Admin
        AccountIdentity adminIdentity = new AccountIdentity("uid-admin-01", "admin@quickbite.com");
        adminIdentity.setPasswordHash(starterHash);
        adminIdentity = identityRepository.save(adminIdentity);
        adminRepository.save(new Admin(adminIdentity, "Platform Operations Admin"));

        // 2. Starter Buyer
        AccountIdentity buyerIdentity = new AccountIdentity("uid-buyer-01", "buyer@quickbite.com");
        buyerIdentity.setPasswordHash(starterHash);
        buyerIdentity = identityRepository.save(buyerIdentity);
        User buyer = new User(buyerIdentity, "Aarav Sharma", "9876543210");
        buyer.updateDeliveryLocation("221B MG Road, Indiranagar, Bengaluru", 12.9784, 77.6408);
        buyer.updateFoodInterests("Paneer, Biryani, Dosa");
        buyer = userRepository.save(buyer);

        // 3. Starter Delivery Partner (verified & active)
        AccountIdentity dpIdentity = new AccountIdentity("uid-dp-01", "delivery@quickbite.com");
        dpIdentity.setPasswordHash(starterHash);
        dpIdentity = identityRepository.save(dpIdentity);
        DeliveryPartner dp = new DeliveryPartner(dpIdentity, "Rohan Patel", "9123456789", "Scooter - KA01QB101", "Central & Indiranagar");
        dp.approveVerification();
        deliveryPartnerRepository.save(dp);

        // 4. Starter Shop Owner + Approved Shop + Menu Items
        AccountIdentity ownerIdentity = new AccountIdentity("uid-owner-01", "shop@quickbite.com");
        ownerIdentity.setPasswordHash(starterHash);
        ownerIdentity = identityRepository.save(ownerIdentity);
        ShopOwner owner = shopOwnerRepository.save(new ShopOwner(ownerIdentity, "Chef Vikram Das", "9988776655"));

        Shop shop = new Shop(
                owner,
                "Urban Spice Kitchen",
                "Freshly prepared wraps, gourmet burgers, South Indian classics and artisanal cold brews.",
                "42 MG Road, Indiranagar, Bengaluru",
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop",
                12.9716,
                77.5946
        );
        shop.approve();
        shop.setOpen(true);
        Shop savedShop = shopRepository.save(shop);

        Product p1 = productRepository.save(new Product(
                savedShop,
                "Paneer Tikka Roll",
                "Charcoal grilled cottage cheese marinated in aromatic spices wrapped in handmade flaky paratha with mint relish.",
                new BigDecimal("120.00"),
                35,
                "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?w=500&auto=format&fit=crop"
        ));
        productRepository.save(new Product(
                savedShop,
                "Crispy Veg Gourmet Burger",
                "Golden potato & corn herb patty, crisp iceberg lettuce, melted cheese and house burger sauce.",
                new BigDecimal("85.00"),
                40,
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop"
        ));
        productRepository.save(new Product(
                savedShop,
                "Mysore Masala Dosa",
                "Crispy fermented crepe with red garlic chili chutney and spiced potato mash served with lentil stew.",
                new BigDecimal("95.00"),
                25,
                "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=500&auto=format&fit=crop"
        ));
        Product p4 = productRepository.save(new Product(
                savedShop,
                "Hyderabadi Dum Biryani Bowl",
                "Long grain basmati rice slow cooked on dum with tender spiced vegetables, saffron and cooling raita.",
                new BigDecimal("150.00"),
                20,
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop"
        ));

        // 5. Initial delivered order & rating for starter shop/buyer
        CustomerOrder sampleOrder = new CustomerOrder(
                "QB-SEED-1001", buyer, savedShop,
                new BigDecimal("390.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Extra mint relish",
                buyer.getDefaultAddress(), buyer.getDefaultLatitude(), buyer.getDefaultLongitude()
        );
        sampleOrder.addItem(new OrderItem(p1, 2));
        sampleOrder.addItem(new OrderItem(p4, 1));
        sampleOrder.transitionTo(OrderStatus.ACCEPTED);
        sampleOrder.transitionTo(OrderStatus.PREPARING);
        sampleOrder.transitionTo(OrderStatus.READY);
        sampleOrder.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
        sampleOrder.transitionTo(OrderStatus.DELIVERED);
        sampleOrder = orderRepository.save(sampleOrder);
        feedbackRepository.save(new Feedback(sampleOrder, buyer, 5, "Best paneer tikka roll and biryani in the neighborhood!"));

        log.info("Starter accounts (1 per role) seeded successfully.");
    }
}
