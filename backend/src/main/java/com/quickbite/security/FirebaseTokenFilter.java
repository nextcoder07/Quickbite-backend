package com.quickbite.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.quickbite.administration.Admin;
import com.quickbite.administration.AdminRepository;
import com.quickbite.buyer.User;
import com.quickbite.buyer.UserRepository;
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
import org.springframework.beans.factory.annotation.Value;
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

    public FirebaseTokenFilter(
            AccountIdentityRepository identityRepository,
            UserRepository userRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            ShopOwnerRepository shopOwnerRepository,
            AdminRepository adminRepository,
            @Value("${quickbite.firebase.enabled:false}") boolean firebaseEnabled) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.shopOwnerRepository = shopOwnerRepository;
        this.adminRepository = adminRepository;
        this.firebaseEnabled = firebaseEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    String uid = null;
                    String email = null;

                    // 1. Support dev / testing tokens or when Firebase is not enabled
                    if (!firebaseEnabled || token.startsWith("dev-token-")) {
                        // Dev token format: dev-token-email@campus.edu or dev-token-role (e.g. dev-token-buyer@quickbite.com)
                        uid = token;
                        if (token.contains("@")) {
                            email = token.replace("dev-token-", "");
                        } else {
                            email = token + "@quickbite.campus";
                        }
                    } else if (!FirebaseApp.getApps().isEmpty()) {
                        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                        uid = decodedToken.getUid();
                        email = decodedToken.getEmail();
                    }

                    if (uid != null) {
                        Optional<AccountIdentity> identityOpt = identityRepository.findByFirebaseUid(uid);
                        if (identityOpt.isEmpty() && email != null) {
                            identityOpt = identityRepository.findByEmailIgnoreCase(email);
                        }

                        if (identityOpt.isPresent()) {
                            AccountIdentity identity = identityOpt.get();
                            CurrentAccountPrincipal principal = resolvePrincipal(identity);
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            // Identity not registered yet: provide unprovisioned principal
                            CurrentAccountPrincipal principal = new CurrentAccountPrincipal(
                                    null, uid, email, null, null, null
                            );
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to authenticate token: {}", ex.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private CurrentAccountPrincipal resolvePrincipal(AccountIdentity identity) {
        Long id = identity.getId();

        // 1. Check Buyer
        Optional<User> buyerOpt = userRepository.findByIdentityId(id);
        if (buyerOpt.isPresent()) {
            User u = buyerOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.BUYER, u.getId(), u.getAccountStatus());
        }

        // 2. Check Delivery Partner
        Optional<DeliveryPartner> dpOpt = deliveryPartnerRepository.findByIdentityId(id);
        if (dpOpt.isPresent()) {
            DeliveryPartner dp = dpOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.DELIVERY_PARTNER, dp.getId(), dp.getAccountStatus());
        }

        // 3. Check Shop Owner
        Optional<ShopOwner> soOpt = shopOwnerRepository.findByIdentityId(id);
        if (soOpt.isPresent()) {
            ShopOwner so = soOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.SHOP_OWNER, so.getId(), so.getAccountStatus());
        }

        // 4. Check Admin
        Optional<Admin> adminOpt = adminRepository.findByIdentityId(id);
        if (adminOpt.isPresent()) {
            Admin a = adminOpt.get();
            return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(),
                    AccountTypeSnapshot.ADMIN, a.getId(), a.getAccountStatus());
        }

        // Registered identity with no profile provisioned yet
        return new CurrentAccountPrincipal(id, identity.getFirebaseUid(), identity.getEmail(), null, null, null);
    }
}
