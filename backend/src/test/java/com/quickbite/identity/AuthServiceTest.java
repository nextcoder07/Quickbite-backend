package com.quickbite.identity;

import com.quickbite.administration.AdminRepository;
import com.quickbite.audit.AuditService;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.enums.AccountTypeSnapshot;
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
                auditService
        );
    }

    @Test
    @DisplayName("Should prohibit public registration of Admin profile")
    void testAdminPublicSignupProhibited() {
        SignupRequest request = new SignupRequest("uid-1", "admin@campus.edu", "Admin User", "123", AccountTypeSnapshot.ADMIN);

        assertThrows(ForbiddenException.class, () -> authService.registerProfile(request));
        verifyNoInteractions(identityRepository);
    }

    @Test
    @DisplayName("Should prevent duplicate profile creation for the same identity")
    void testProfileExclusivityEnforcement() {
        AccountIdentity identity = new AccountIdentity("uid-2", "buyer@campus.edu");
        when(identityRepository.findByFirebaseUid("uid-2")).thenReturn(Optional.of(identity));
        when(userRepository.existsByIdentityId(identity.getId())).thenReturn(true);

        SignupRequest request = new SignupRequest("uid-2", "buyer@campus.edu", "Buyer", "123", AccountTypeSnapshot.DELIVERY_PARTNER);

        assertThrows(ConflictException.class, () -> authService.registerProfile(request));
    }
}
