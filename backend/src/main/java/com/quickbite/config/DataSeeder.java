package com.quickbite.config;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.shop.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
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

    public DataSeeder(
            AccountIdentityRepository identityRepository,
            AdminRepository adminRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            ShopRepository shopRepository,
            ProductRepository productRepository) {
        this.identityRepository = identityRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminRepository.count() > 0) {
            log.info("Database already seeded. Skipping dev seed.");
            return;
        }

        log.info("Starting safe local development data seeding...");

        // 1. Seed Admin
        AccountIdentity adminIdentity = identityRepository.save(new AccountIdentity("uid-admin-01", "admin@quickbite.campus"));
        adminRepository.save(new Admin(adminIdentity, "Campus Operations Admin"));

        // 2. Seed Buyer
        AccountIdentity buyerIdentity = identityRepository.save(new AccountIdentity("uid-buyer-01", "buyer@quickbite.campus"));
        userRepository.save(new User(buyerIdentity, "Aarav Sharma", "9876543210"));

        // 3. Seed Delivery Partner
        AccountIdentity dpIdentity = identityRepository.save(new AccountIdentity("uid-dp-01", "delivery@quickbite.campus"));
        deliveryPartnerRepository.save(new DeliveryPartner(dpIdentity, "Rohan Patel (Campus Express)", "9123456789"));

        // 4. Seed Shop Owner & Approved Open Shop
        AccountIdentity shopOwnerIdentity = identityRepository.save(new AccountIdentity("uid-owner-01", "shop@quickbite.campus"));
        ShopOwner shopOwner = shopOwnerRepository.save(new ShopOwner(shopOwnerIdentity, "Chef Vikram Das", "9988776655"));

        Shop shop = new Shop(
                shopOwner,
                "Campus Central Cafeteria",
                "Authentic freshly prepared meals, wraps, gourmet burgers and artisanal campus beverages.",
                "Student Activity Center (SAC) - Ground Floor, Booth 4",
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop"
        );
        shop.approve();
        shop.setOpen(true);
        Shop savedShop = shopRepository.save(shop);

        // 5. Seed Catalog Products
        productRepository.save(new Product(
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
                "Golden potato & corn herb patty, farm fresh iceberg lettuce, melted cheese and secret house burger sauce.",
                new BigDecimal("85.00"),
                40,
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop"
        ));

        productRepository.save(new Product(
                savedShop,
                "Mysore Masala Dosa",
                "Crispy fermented crepe with red garlic chili chutney and spiced potato mash, served with piping hot lentil stew.",
                new BigDecimal("95.00"),
                25,
                "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=500&auto=format&fit=crop"
        ));

        productRepository.save(new Product(
                savedShop,
                "Iced Caramel Cold Brew",
                "Slow steeped arabica coffee with chilled milk, caramel drizzle and Madagascar vanilla ice cream.",
                new BigDecimal("75.00"),
                50,
                "https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=500&auto=format&fit=crop"
        ));

        productRepository.save(new Product(
                savedShop,
                "Hyderabadi Dum Biryani Bowl",
                "Long grain basmati rice slow cooked on dum with tender spiced vegetables, saffron, crispy onions and cooling raita.",
                new BigDecimal("150.00"),
                20,
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop"
        ));

        log.info("Development data seed completed successfully!");
    }
}
