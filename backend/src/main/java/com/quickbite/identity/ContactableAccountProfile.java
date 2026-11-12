package com.quickbite.identity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Intermediate OOP base class for profiles with a phone contact number.
 * Extended by User (Buyer), ShopOwner, and DeliveryPartner.
 */
@MappedSuperclass
public abstract class ContactableAccountProfile extends BaseAccountProfile {

    @Column(name = "phone", length = 20)
    private String phone;

    protected ContactableAccountProfile() {
        super();
    }

    protected ContactableAccountProfile(AccountIdentity identity, String displayName, String phone) {
        super(identity, displayName);
        this.phone = phone != null ? phone.trim() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void updateProfile(String displayName, String phone) {
        updateDisplayName(displayName);
        if (phone != null) {
            this.phone = phone.trim();
            touchUpdatedAt();
        }
    }
}

