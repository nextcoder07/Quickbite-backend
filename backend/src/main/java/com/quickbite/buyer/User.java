package com.quickbite.buyer;

import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.ContactableAccountProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends ContactableAccountProfile {

    @Column(name = "default_address", length = 500)
    private String defaultAddress;

    @Column(name = "default_latitude")
    private Double defaultLatitude;

    @Column(name = "default_longitude")
    private Double defaultLongitude;

    @Column(name = "food_interests", length = 500)
    private String foodInterests;

    protected User() {
        super();
    }

    public User(AccountIdentity identity, String displayName, String phone) {
        super(identity, displayName, phone);
    }

    public String getDefaultAddress() {
        return defaultAddress;
    }

    public Double getDefaultLatitude() {
        return defaultLatitude;
    }

    public Double getDefaultLongitude() {
        return defaultLongitude;
    }

    public String getFoodInterests() {
        return foodInterests;
    }

    public void updateDeliveryLocation(String defaultAddress, Double latitude, Double longitude) {
        if (defaultAddress != null) {
            this.defaultAddress = defaultAddress.trim();
        }
        this.defaultLatitude = latitude;
        this.defaultLongitude = longitude;
        touchUpdatedAt();
    }

    public void updateFoodInterests(String foodInterests) {
        this.foodInterests = foodInterests != null ? foodInterests.trim() : null;
        touchUpdatedAt();
    }

    @Override
    public AccountTypeSnapshot getAccountType() {
        return AccountTypeSnapshot.BUYER;
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
