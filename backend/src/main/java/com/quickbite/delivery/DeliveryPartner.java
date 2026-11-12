package com.quickbite.delivery;

import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.ContactableAccountProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_partners")
public class DeliveryPartner extends ContactableAccountProfile {

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "vehicle_details", length = 150)
    private String vehicleDetails;

    @Column(name = "service_area", length = 150)
    private String serviceArea;

    protected DeliveryPartner() {
        super();
    }

    public DeliveryPartner(AccountIdentity identity, String displayName, String phone) {
        this(identity, displayName, phone, null, null);
    }

    public DeliveryPartner(AccountIdentity identity, String displayName, String phone,
                           String vehicleDetails, String serviceArea) {
        super(identity, displayName, phone);
        this.verificationStatus = VerificationStatus.PENDING;
        this.vehicleDetails = vehicleDetails != null ? vehicleDetails.trim() : null;
        this.serviceArea = serviceArea != null ? serviceArea.trim() : null;
    }

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.PENDING;
        }
    }

    @Override
    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getVehicleDetails() {
        return vehicleDetails;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void updateCourierDetails(String vehicleDetails, String serviceArea) {
        if (vehicleDetails != null) {
            this.vehicleDetails = vehicleDetails.trim();
        }
        if (serviceArea != null) {
            this.serviceArea = serviceArea.trim();
        }
        touchUpdatedAt();
    }

    public void approveVerification() {
        this.verificationStatus = VerificationStatus.APPROVED;
        reactivate();
    }

    public void rejectVerification() {
        this.verificationStatus = VerificationStatus.REJECTED;
        touchUpdatedAt();
    }

    @Override
    public void suspend() {
        super.suspend();
        this.verificationStatus = VerificationStatus.SUSPENDED;
    }

    @Override
    public void reactivate() {
        super.reactivate();
        if (this.verificationStatus == VerificationStatus.SUSPENDED || this.verificationStatus == VerificationStatus.PENDING) {
            this.verificationStatus = VerificationStatus.APPROVED;
        }
    }

    public boolean isVerifiedAndActive() {
        return isActive() && this.verificationStatus == VerificationStatus.APPROVED;
    }

    @Override
    public AccountTypeSnapshot getAccountType() {
        return AccountTypeSnapshot.DELIVERY_PARTNER;
    }

    @Override
    public boolean canOperateOnPlatform() {
        return isVerifiedAndActive();
    }
}
