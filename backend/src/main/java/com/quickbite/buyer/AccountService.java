package com.quickbite.buyer;

import com.quickbite.audit.AuditService;
import com.quickbite.buyer.dto.UpdateProfileRequest;
import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.exception.ForbiddenException;
import com.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.security.CurrentAccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AccountService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public User updateProfile(CurrentAccountPrincipal principal, UpdateProfileRequest request) {
        User user = getUser(principal);
        user.updateProfile(request.displayName(), request.phone());
        auditService.record(user.getIdentity(), "PROFILE_UPDATED", "USER", String.valueOf(user.getId()),
                Map.of("displayName", request.displayName()));
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(CurrentAccountPrincipal principal) {
        User user = getUser(principal);
        if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
            return;
        }
        user.deactivate();
        userRepository.save(user);
        auditService.record(user.getIdentity(), "ACCOUNT_DEACTIVATED", "USER", String.valueOf(user.getId()),
                Map.of("reason", "Self-deactivated by buyer"));
    }

    private User getUser(CurrentAccountPrincipal principal) {
        if (principal == null || principal.getIdentityId() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userRepository.findByIdentityId(principal.getIdentityId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));
    }
}
