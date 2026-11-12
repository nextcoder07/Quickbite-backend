package com.quickbite.identity;

import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminKeyValidator;
import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.common.exception.ConflictException;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.shop.ShopOwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AccountIdentityRepository identityRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeliveryPartnerRepository deliveryPartnerRepository;
    @Mock private ShopOwnerRepository shopOwnerRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                identityRepository,
                userRepository,
                deliveryPartnerRepository,
                shopOwnerRepository,
                adminRepository,
                auditService,
                new AdminKeyValidator("SecretAdminEnvKey123")
        );
    }

    @Test
    @DisplayName("Should reject Admin signup when provided env key is invalid")
    void testAdminSignupInvalidKeyRejected() {
        SignupRequest request = new SignupRequest(
                "uid-1", "admin@campus.edu", "Admin User", null,
                AccountTypeSnapshot.ADMIN, "Secret123!", null, null, null, null, null, null,
                "WrongKey", null
        );

        assertThrows(ForbiddenException.class, () -> authService.registerProfile(request));
        verifyNoInteractions(identityRepository);
    }

    @Test
    @DisplayName("Should create APPROVED Admin immediately when valid env key is provided")
    void testAdminSignupWithValidEnvKey() {
        AccountIdentity savedIdentity = new AccountIdentity(10L, "uid-10", "newadmin@campus.edu");
        when(identityRepository.findByFirebaseUid("uid-10")).thenReturn(Optional.empty());
        when(identityRepository.findByEmailIgnoreCase("newadmin@campus.edu")).thenReturn(Optional.empty());
        when(identityRepository.saveAndFlush(any(AccountIdentity.class))).thenReturn(savedIdentity);
        when(adminRepository.saveAndFlush(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adminRepository.findByIdentityId(10L)).thenAnswer(inv -> {
            Admin a = new Admin(savedIdentity, "New Admin", VerificationStatus.APPROVED, null);
            return Optional.of(a);
        });

        SignupRequest request = new SignupRequest(
                "uid-10", "newadmin@campus.edu", "New Admin", null,
                AccountTypeSnapshot.ADMIN, "Secret123!", null, null, null, null, null, null,
                "SecretAdminEnvKey123", null
        );

        AuthResponse response = authService.registerProfile(request);
        assertEquals(AccountTypeSnapshot.ADMIN, response.accountType());
        assertEquals(VerificationStatus.APPROVED, response.verificationStatus());
    }

    @Test
    @DisplayName("Should create PENDING Admin awaiting approval when no env key is provided")
    void testAdminSignupWithoutKeyCreatesPendingRequest() {
        AccountIdentity savedIdentity = new AccountIdentity(11L, "uid-11", "pendingadmin@campus.edu");
        when(identityRepository.findByFirebaseUid("uid-11")).thenReturn(Optional.empty());
        when(identityRepository.findByEmailIgnoreCase("pendingadmin@campus.edu")).thenReturn(Optional.empty());
        when(identityRepository.saveAndFlush(any(AccountIdentity.class))).thenReturn(savedIdentity);
        when(adminRepository.saveAndFlush(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adminRepository.findByIdentityId(11L)).thenAnswer(inv -> {
            Admin a = new Admin(savedIdentity, "Applicant Admin", VerificationStatus.PENDING, "Joining ops");
            return Optional.of(a);
        });

        SignupRequest request = new SignupRequest(
                "uid-11", "pendingadmin@campus.edu", "Applicant Admin", null,
                AccountTypeSnapshot.ADMIN, "Secret123!", null, null, null, null, null, null,
                null, "Joining ops"
        );

        AuthResponse response = authService.registerProfile(request);
        assertEquals(AccountTypeSnapshot.ADMIN, response.accountType());
        assertEquals(VerificationStatus.PENDING, response.verificationStatus());
    }

    @Test
    @DisplayName("Should prevent duplicate profile creation for the same identity")
    void testProfileExclusivityEnforcement() {
        AccountIdentity identity = new AccountIdentity(2L, "uid-2", "buyer@campus.edu");
        when(identityRepository.findByFirebaseUid("uid-2")).thenReturn(Optional.of(identity));
        when(userRepository.existsByIdentityId(identity.getId())).thenReturn(true);

        SignupRequest request = new SignupRequest("uid-2", "buyer@campus.edu", "Buyer", "123", AccountTypeSnapshot.DELIVERY_PARTNER);

        assertThrows(ConflictException.class, () -> authService.registerProfile(request));
    }
}
