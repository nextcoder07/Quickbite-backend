package com.quickbite.shop;

import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.ContactableAccountProfile;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "shop_owners")
public class ShopOwner extends ContactableAccountProfile {

    protected ShopOwner() {
        super();
    }

    public ShopOwner(AccountIdentity identity, String displayName, String phone) {
        super(identity, displayName, phone);
    }

    @Override
    public AccountTypeSnapshot getAccountType() {
        return AccountTypeSnapshot.SHOP_OWNER;
    }

    @Override
    public VerificationStatus getVerificationStatus() {
        return VerificationStatus.APPROVED;
    }

    @Override
    public boolean canOperateOnPlatform() {
        return isActive();
    }
}
