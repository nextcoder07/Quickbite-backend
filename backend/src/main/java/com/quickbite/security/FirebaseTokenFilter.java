package com.quickbite.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
import com.quickbite.common.dto.ErrorResponse;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.delivery.DeliveryPartner;
import com.quickbite.delivery.DeliveryPartnerRepository;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.AccountIdentityRepository;
import com.quickbite.shop.ShopOwner;
import com.quickbite.shop.ShopOwnerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);

    private final AccountIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ShopOwnerRepository shopOwnerRepository;
    private final AdminRepository adminRepository;
    private final boolean firebaseEnabled;
    private final ObjectMapper objectMapper;

    public FirebaseTokenFilter(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            @Value("${quickbite.firebase.enabled:true}") boolean firebaseEnabled) {
        this(identityRepository, userRepository, deliveryPartnerRepository, shopOwnerRepository,
                adminRepository, firebaseEnabled, new ObjectMapper().findAndRegisterModules());
    }

    @Autowired
    public FirebaseTokenFilter(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            @Value("${quickbite.firebase.enabled:true}") boolean firebaseEnabled,
            ObjectMapper objectMapper) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.adminRepository = adminRepository;
        this.firebaseEnabled = firebaseEnabled;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    if (!firebaseEnabled) {
                        writeUnauthorizedError(request, response,
                                "Firebase token verification cannot be bypassed.");
                        return;
                    }
                    if (FirebaseApp.getApps().isEmpty()) {
                        writeUnauthorizedError(request, response,
                                "Firebase Admin SDK is not initialized on the backend.");
                        return;
                    }
                    FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                    String uid = decodedToken.getUid();
                    String email = decodedToken.getEmail();

                    if (uid == null || uid.isBlank()) {
                        writeUnauthorizedError(request, response, "Firebase token does not contain a valid UID.");
                        return;
                    }

                    Optional<AccountIdentity> identityOpt = identityRepository.findByFirebaseUid(uid);
                    if (identityOpt.isEmpty() && email != null) {
                        identityOpt = identityRepository.findByEmailIgnoreCase(email);
                    }

                    if (identityOpt.isPresent()) {
                        AccountIdentity identity = identityOpt.get();
                        CurrentAccountPrincipal principal = resolvePrincipal(identity);
                        if (principal.getStatus() == AccountStatus.DEACTIVATED) {
                            SecurityContextHolder.clearContext();
                            writeUnauthorizedError(request, response,
                                    "This account has been deactivated and removed from the platform.");
                            return;
                        }
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // Authenticated in Firebase, not yet provisioned in Supabase (used for /auth/signup)
                        CurrentAccountPrincipal principal = new CurrentAccountPrincipal(
                                null, uid, email, null, null, null
                        );
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (FirebaseAuthException ex) {
                    log.warn("Firebase token verification failed: {}", ex.getMessage());
                    SecurityContextHolder.clearContext();
                    writeUnauthorizedError(request, response, "Firebase authentication failed: " + ex.getMessage());
                    return;
                } catch (Exception ex) {
                    log.warn("Token authentication error: {}", ex.getMessage());
                    SecurityContextHolder.clearContext();
                    writeUnauthorizedError(request, response, "Authentication error: " + ex.getMessage());
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedError(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private CurrentAccountPrincipal resolvePrincipal(AccountIdentity identity) {
        Long id = identity.getId();

        // 1. Check Admin first (supports promoted buyers as well as direct admins)
        Optional<Admin> adminOpt = adminRepository.findByIdentityId(id);
        if (adminOpt.isPresent()) {
            Admin a = adminOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.ADMIN, a.getId(), a.getAccountStatus(), a.getVerificationStatus());
        }

        // 2. Check Buyer
        Optional<User> buyerOpt = userRepository.findByIdentityId(id);
        if (buyerOpt.isPresent()) {
            User u = buyerOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.BUYER, u.getId(), u.getAccountStatus(), u.getVerificationStatus());
        }

        // 3. Check Delivery Partner
        Optional<DeliveryPartner> dpOpt = deliveryPartnerRepository.findByIdentityId(id);
        if (dpOpt.isPresent()) {
            DeliveryPartner dp = dpOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.DELIVERY_PARTNER, dp.getId(), dp.getAccountStatus(), dp.getVerificationStatus());
        }

        // 4. Check Shop Owner
        Optional<ShopOwner> soOpt = shopOwnerRepository.findByIdentityId(id);
        if (soOpt.isPresent()) {
            ShopOwner so = soOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.SHOP_OWNER, so.getId(), so.getAccountStatus(), so.getVerificationStatus());
        }

        // Registered identity with no profile provisioned yet
        return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(), null, null, null, null);
    }
}
