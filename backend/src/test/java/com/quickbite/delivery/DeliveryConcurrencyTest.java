package com.quickbite.delivery;

import com.quickbite.accountdeletion.AccountDeletionService;
import com.quickbite.accountdeletion.dto.CreateDeletionRequest;
import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.DeliveryStatus;
import com.quickbite.common.enums.OrderStatus;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.ordering.CustomerOrder;
import com.quickbite.ordering.CustomerOrderRepository;
import com.quickbite.security.CurrentAccountPrincipal;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
import com.quickbite.shop.ShopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryConcurrencyTest {

    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Autowired private DeliveryPartnerRepository deliveryPartnerRepository;
    @Autowired private AccountIdentityRepository identityRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopOwnerRepository shopOwnerRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CustomerOrderRepository orderRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AccountDeletionService deletionService;

    @Test
    @DisplayName("Database row lock ensures strictly one delivery partner wins concurrent claim")
    void testConcurrentDeliveryClaimDatabaseLock() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        AccountIdentity buyerId = identityRepository.save(new AccountIdentity("b-" + suffix, "buyer-" + suffix + "@test.com"));
        User buyer = userRepository.save(new User(buyerId, "Test Buyer", "9990001111"));

        AccountIdentity ownerId = identityRepository.save(new AccountIdentity("o-" + suffix, "owner-" + suffix + "@test.com"));
        ShopOwner owner = shopOwnerRepository.save(new ShopOwner(ownerId, "Test Owner", "9990002222"));

        Shop shop = new Shop(owner, "Anywhere Eats " + suffix, "Global kitchen", "10 Downing Street, London", null, 51.5034, -0.1276);
        shop.approve();
        shop.setOpen(true);
        shop = shopRepository.save(shop);

        CustomerOrder order = new CustomerOrder(
                "QB-RACE-" + suffix,
                buyer,
                shop,
                new BigDecimal("250.00"),
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                "Ring doorbell",
                "22 Baker Street, London",
                51.5238,
                -0.1586
        );
        order.transitionTo(OrderStatus.ACCEPTED);
        order.transitionTo(OrderStatus.PREPARING);
        order.transitionTo(OrderStatus.READY);
        order = orderRepository.save(order);

        DeliveryAssignment assignment = deliveryAssignmentRepository.save(new DeliveryAssignment(order));
        Long assignmentId = assignment.getId();

        // Create 5 verified delivery partners racing to claim the same assignment
        int threadCount = 5;
        List<CurrentAccountPrincipal> principals = new ArrayList<>();
        List<Long> partnerIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            AccountIdentity dpIdentity = identityRepository.save(
                    new AccountIdentity("dp-" + suffix + "-" + i, "dp" + i + "-" + suffix + "@test.com")
            );
            DeliveryPartner partner = new DeliveryPartner(dpIdentity, "Courier " + i, "900000000" + i, "Bike", "Central");
            partner.approveVerification();
            partner = deliveryPartnerRepository.save(partner);
            partnerIds.add(partner.getId());
            principals.add(new CurrentAccountPrincipal(
                    dpIdentity.getId(), dpIdentity.getFirebaseUid(), dpIdentity.getEmail(),
                    AccountTypeSnapshot.DELIVERY_PARTNER, partner.getId(), partner.getAccountStatus()
            ));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        List<Long> winningPartnerIds = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final CurrentAccountPrincipal p = principals.get(i);
            final Long pId = partnerIds.get(i);
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    deliveryService.claimAssignment(p, assignmentId);
                    successes.incrementAndGet();
                    winningPartnerIds.add(pId);
                } catch (ConflictException ex) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        go.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, successes.get(), "Exactly ONE delivery partner must claim the assignment");
        assertEquals(threadCount - 1, conflicts.get(), "All other concurrent partners must receive ConflictException");

        // Verify persisted DB state directly
        DeliveryAssignment fromDb = deliveryAssignmentRepository.findById(assignmentId).orElseThrow();
        assertEquals(DeliveryStatus.ACCEPTED, fromDb.getStatus());
        assertNotNull(fromDb.getDeliveryPartner());
        assertEquals(winningPartnerIds.get(0), fromDb.getDeliveryPartner().getId());
    }

    @Test
    @DisplayName("Unverified delivery partner is blocked until admin verifies, and shop removal needs admin confirmation")
    void testVerificationGateAndAdminConfirmedRemoval() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        AccountIdentity pendingDpId = identityRepository.save(new AccountIdentity("pdp-" + suffix, "pdp-" + suffix + "@test.com"));
        DeliveryPartner pendingDp = deliveryPartnerRepository.save(new DeliveryPartner(pendingDpId, "Pending Courier", "9111111111"));
        assertEquals(VerificationStatus.PENDING, pendingDp.getVerificationStatus());

        CurrentAccountPrincipal pendingPrincipal = new CurrentAccountPrincipal(
                pendingDpId.getId(), pendingDpId.getFirebaseUid(), pendingDpId.getEmail(),
                AccountTypeSnapshot.DELIVERY_PARTNER, pendingDp.getId(), pendingDp.getAccountStatus()
        );
        assertThrows(ForbiddenException.class, () -> deliveryService.getAvailableDeliveries(pendingPrincipal));

        // Create Admin + Shop Owner requesting removal
        AccountIdentity adminIdentity = identityRepository.save(new AccountIdentity("adm-" + suffix, "adm-" + suffix + "@test.com"));
        Admin admin = adminRepository.save(new Admin(adminIdentity, "Ops Admin"));
        CurrentAccountPrincipal adminPrincipal = new CurrentAccountPrincipal(
                adminIdentity.getId(), adminIdentity.getFirebaseUid(), adminIdentity.getEmail(),
                AccountTypeSnapshot.ADMIN, admin.getId(), admin.getAccountStatus()
        );

        AccountIdentity ownerIdentity = identityRepository.save(new AccountIdentity("own-" + suffix, "own-" + suffix + "@test.com"));
        ShopOwner owner = shopOwnerRepository.save(new ShopOwner(ownerIdentity, "Shop Owner", "9222222222"));
        Shop shop = new Shop(owner, "Removable Bistro " + suffix, "Desc", "Main Street, NYC", null);
        shop.approve();
        shop.setOpen(true);
        shop = shopRepository.save(shop);

        CurrentAccountPrincipal ownerPrincipal = new CurrentAccountPrincipal(
                ownerIdentity.getId(), ownerIdentity.getFirebaseUid(), ownerIdentity.getEmail(),
                AccountTypeSnapshot.SHOP_OWNER, owner.getId(), owner.getAccountStatus()
        );

        var req = deletionService.submitRequest(ownerPrincipal, new CreateDeletionRequest("Closing restaurant"));
        // Before admin confirms, shop and owner remain active
        assertTrue(shopOwnerRepository.findById(owner.getId()).orElseThrow().isActive());
        assertFalse(shopRepository.findById(shop.getId()).orElseThrow().isDeactivated());

        // After admin confirms, both owner and shop are deactivated
        deletionService.reviewRequest(adminPrincipal, req.id(), true, "Confirmed");
        ShopOwner updatedOwner = shopOwnerRepository.findById(owner.getId()).orElseThrow();
        Shop updatedShop = shopRepository.findById(shop.getId()).orElseThrow();
        assertEquals(AccountStatus.DEACTIVATED, updatedOwner.getAccountStatus());
        assertTrue(updatedShop.isDeactivated());
        assertFalse(updatedShop.isOpen());
    }
}

