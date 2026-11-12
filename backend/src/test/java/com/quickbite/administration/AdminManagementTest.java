package com.quickbite.administration;

import com.quickbite.administration.dto.AdminAccountResponse;
import com.quickbite.administration.dto.AdminRoleRequestResponse;
import com.quickbite.buyer.AccountService;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.identity.AuthService;
import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admintests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "quickbite.admin.signup-key=TestAdminEnvKey@999"
})
@Transactional
class AdminManagementTest {

    @Autowired private AuthService authService;
    @Autowired private AdminService adminService;
    @Autowired private AccountService accountService;
    @Autowired private AccountIdentityRepository identityRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private UserRepository userRepository;

    private CurrentAccountPrincipal firebasePrincipal(String uid, String email) {
        return new CurrentAccountPrincipal(null, uid, email, null, null, null, null);
    }

    @Test
    @DisplayName("Suspending an admin requires valid env key; approving pending admin does not")
    void testAdminApprovalAndKeyGatedSuspension() {
        Admin primaryAdmin = adminRepository.findAll().stream()
                .filter(Admin::isVerifiedAndActive)
                .findFirst()
                .orElseThrow();
        CurrentAccountPrincipal primaryPrincipal = new CurrentAccountPrincipal(
                primaryAdmin.getIdentity().getId(),
                primaryAdmin.getIdentity().getFirebaseUid(),
                primaryAdmin.getIdentity().getEmail(),
                AccountTypeSnapshot.ADMIN,
                primaryAdmin.getId(),
                AccountStatus.ACTIVE,
                VerificationStatus.APPROVED
        );

        // 1. New signup asks for ADMIN without env key -> PENDING
        AuthResponse pendingResp = authService.registerFirebasePrincipal(
                firebasePrincipal("uid-pending-admin-test", "pending.admin@quickbite.com"),
                new SignupRequest(
                        null, "pending.admin@quickbite.com", "Pending Admin", null,
                        AccountTypeSnapshot.ADMIN, null, null, null, null, null, null, null,
                        null, "Need admin access for operations"
                ));
        assertEquals(VerificationStatus.PENDING, pendingResp.verificationStatus());

        // Pending admin cannot perform admin operations
        CurrentAccountPrincipal pendingPrincipal = new CurrentAccountPrincipal(
                pendingResp.identityId(), pendingResp.firebaseUid(), pendingResp.email(),
                AccountTypeSnapshot.ADMIN, pendingResp.profileId(), AccountStatus.ACTIVE, VerificationStatus.PENDING
        );
        assertThrows(ForbiddenException.class, () -> adminService.getSummary(pendingPrincipal));

        // 2. Primary admin approves pending admin
        AdminAccountResponse approved = adminService.reviewAdminStatus(
                primaryPrincipal, pendingResp.profileId(), "APPROVED", null
        );
        assertEquals(VerificationStatus.APPROVED, approved.verificationStatus());

        // 3. Suspending admin without key or wrong key fails
        assertThrows(ForbiddenException.class, () ->
                adminService.reviewAdminStatus(primaryPrincipal, approved.id(), "SUSPENDED", null));
        assertThrows(ForbiddenException.class, () ->
                adminService.reviewAdminStatus(primaryPrincipal, approved.id(), "SUSPENDED", "WrongKey"));

        // 4. Suspending admin with valid env key succeeds
        AdminAccountResponse suspended = adminService.reviewAdminStatus(
                primaryPrincipal, approved.id(), "SUSPENDED", "TestAdminEnvKey@999"
        );
        assertEquals(AccountStatus.SUSPENDED, suspended.accountStatus());
        assertEquals(VerificationStatus.SUSPENDED, suspended.verificationStatus());
    }

    @Test
    @DisplayName("Existing buyer can request admin permission and be promoted by an active admin")
    void testExistingBuyerRequestsAndReceivesAdminRole() {
        Admin primaryAdmin = adminRepository.findAll().stream()
                .filter(Admin::isVerifiedAndActive)
                .findFirst()
                .orElseThrow();
        CurrentAccountPrincipal primaryPrincipal = new CurrentAccountPrincipal(
                primaryAdmin.getIdentity().getId(),
                primaryAdmin.getIdentity().getFirebaseUid(),
                primaryAdmin.getIdentity().getEmail(),
                AccountTypeSnapshot.ADMIN,
                primaryAdmin.getId(),
                AccountStatus.ACTIVE,
                VerificationStatus.APPROVED
        );

        AuthResponse buyerResp = authService.registerFirebasePrincipal(
                firebasePrincipal("uid-buyer-to-admin-test", "buyer.to.admin@quickbite.com"),
                new SignupRequest(
                        null, "buyer.to.admin@quickbite.com", "Promoted Buyer", "9999999999",
                        AccountTypeSnapshot.BUYER, null, null, null, null, null, null, null,
                        null, null
                ));
        assertEquals(AccountTypeSnapshot.BUYER, buyerResp.accountType());

        CurrentAccountPrincipal buyerPrincipal = new CurrentAccountPrincipal(
                buyerResp.identityId(), buyerResp.firebaseUid(), buyerResp.email(),
                AccountTypeSnapshot.BUYER, buyerResp.profileId(), AccountStatus.ACTIVE, VerificationStatus.APPROVED
        );

        AdminRoleRequestResponse req = accountService.requestAdminRole(buyerPrincipal, "Helping manage vendors");
        assertEquals(VerificationStatus.PENDING, req.status());

        AdminRoleRequestResponse approvedReq = adminService.reviewAdminRoleRequest(
                primaryPrincipal, req.id(), true, "Approved for ops"
        );
        assertEquals(VerificationStatus.APPROVED, approvedReq.status());

        AccountIdentity identity = identityRepository.findById(buyerResp.identityId()).orElseThrow();
        AuthResponse resolved = authService.resolveAuthResponse(identity);
        assertEquals(AccountTypeSnapshot.ADMIN, resolved.accountType());
        assertEquals(VerificationStatus.APPROVED, resolved.verificationStatus());
        assertTrue(userRepository.existsByIdentityId(identity.getId()));
    }

    @Test
    @DisplayName("Login requires no key; verified accounts open approved while pending accounts remain pending until verified")
    void loginRequiresNoKeyForVerifiedAndKeepsPendingAccountsGated() {
        AuthResponse seededAdminLogin = authService.loginWithFirebasePrincipal(
                firebasePrincipal("uid-admin-01", "admin@quickbite.com"));
        assertEquals(AccountTypeSnapshot.ADMIN, seededAdminLogin.accountType());
        assertEquals(VerificationStatus.APPROVED, seededAdminLogin.verificationStatus());
        assertEquals(AccountStatus.ACTIVE, seededAdminLogin.status());

        AuthResponse pendingAdmin = authService.registerFirebasePrincipal(
                firebasePrincipal("uid-pending-login-admin", "pending.login.admin@quickbite.com"),
                new SignupRequest(
                        null, "pending.login.admin@quickbite.com", "Pending Login Admin", "9888877777",
                        AccountTypeSnapshot.ADMIN, null, null, null, null, null, null, null,
                        null, "Awaiting verification"
                ));
        assertEquals(VerificationStatus.PENDING, pendingAdmin.verificationStatus());

        AuthResponse pendingLogin = authService.loginWithFirebasePrincipal(
                firebasePrincipal("uid-pending-login-admin", "pending.login.admin@quickbite.com"));
        assertEquals(AccountTypeSnapshot.ADMIN, pendingLogin.accountType());
        assertEquals(VerificationStatus.PENDING, pendingLogin.verificationStatus());
        assertEquals(AccountStatus.ACTIVE, pendingLogin.status());
    }

    @Test
    @DisplayName("Pending Admin can verify from waiting gate via AccountService.verifyAdminWithKey")
    void verifyAdminWithKeyApprovesPendingAdminAndRejectsInvalidKey() {
        AuthResponse pendingAdmin = authService.registerFirebasePrincipal(
                firebasePrincipal("uid-gate-key-admin", "gate.key.admin@quickbite.com"),
                new SignupRequest(
                        null, "gate.key.admin@quickbite.com", "Gate Key Admin", "9777766666",
                        AccountTypeSnapshot.ADMIN, null, null, null, null, null, null, null,
                        null, "Waiting at gate"
                ));
        CurrentAccountPrincipal pendingPrincipal = new CurrentAccountPrincipal(
                pendingAdmin.identityId(), pendingAdmin.firebaseUid(), pendingAdmin.email(),
                AccountTypeSnapshot.ADMIN, pendingAdmin.profileId(), AccountStatus.ACTIVE, VerificationStatus.PENDING
        );

        assertThrows(ForbiddenException.class, () ->
                accountService.verifyAdminWithKey(pendingPrincipal, "bad-key")
        );

        Map<String, String> result = accountService.verifyAdminWithKey(pendingPrincipal, "TestAdminEnvKey@999");
        assertEquals("APPROVED", result.get("verificationStatus"));

        AccountIdentity identity = identityRepository.findById(pendingAdmin.identityId()).orElseThrow();
        AuthResponse refreshed = authService.resolveAuthResponse(identity);
        assertEquals(VerificationStatus.APPROVED, refreshed.verificationStatus());
    }
}

