package com.quickbite.identity.dto;

import com.quickbite.common.enums.AccountStatus;
import com.quickbite.common.enums.AccountTypeSnapshot;

public record AuthResponse(
        Long identityId,
        String firebaseUid,
        String email,
        AccountTypeSnapshot accountType,
        Long profileId,
        String displayName,
        String phone,
        AccountStatus status,
        String token
) {}
