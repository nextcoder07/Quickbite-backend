package com.quickbite.security;

import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CurrentAccountPrincipal implements UserDetails {

    private final Long identityId;
    private final String firebaseUid;
    private final String email;
    private final AccountTypeSnapshot accountType;
    private final Long profileId;
    private final AccountStatus status;
    private final VerificationStatus verificationStatus;

    public CurrentAccountPrincipal(Long identityId, String firebaseUid, String email,
                                   AccountTypeSnapshot accountType, Long profileId, AccountStatus status) {
        this(identityId, firebaseUid, email, accountType, profileId, status, VerificationStatus.APPROVED);
    }

    public CurrentAccountPrincipal(Long identityId, String firebaseUid, String email,
                                   AccountTypeSnapshot accountType, Long profileId, AccountStatus status,
                                   VerificationStatus verificationStatus) {
        this.identityId = identityId;
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.accountType = accountType;
        this.profileId = profileId;
        this.status = status;
        this.verificationStatus = verificationStatus;
    }

    public Long getIdentityId() {
        return identityId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public AccountTypeSnapshot getAccountType() {
        return accountType;
    }

    public Long getProfileId() {
        return profileId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (accountType == null) {
            return Collections.emptyList();
        }
        if (accountType == AccountTypeSnapshot.ADMIN
                && (status != AccountStatus.ACTIVE || verificationStatus != VerificationStatus.APPROVED)) {
            return Collections.emptyList();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + accountType.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email != null ? email : firebaseUid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }
}
