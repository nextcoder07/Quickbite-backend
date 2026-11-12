package com.quickbite.administration;

import com.quickbite.common.enums.AccountTypeSnapshot;
import com.quickbite.common.enums.VerificationStatus;
import com.quickbite.identity.AccountIdentity;
import com.quickbite.identity.BaseAccountProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends BaseAccountProfile {

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "request_note", length = 500)
    private String requestNote;

    protected Admin() {
        super();
    }

    public Admin(AccountIdentity identity, String displayName) {
        this(identity, displayName, VerificationStatus.APPROVED, null);
    }

    public Admin(AccountIdentity identity, String displayName, VerificationStatus verificationStatus, String requestNote) {
        super(identity, displayName);
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
        this.requestNote = requestNote != null ? requestNote.trim() : null;
    }

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.APPROVED;
        }
    }

    @Override
    public AccountTypeSnapshot getAccountType() {
        return AccountTypeSnapshot.ADMIN;
    }

    @Override
    public VerificationStatus getVerificationStatus() {
        return verificationStatus != null ? verificationStatus : VerificationStatus.APPROVED;
    }

    public String getRequestNote() {
        return requestNote;
    }

    public void approveVerification() {
        this.verificationStatus = VerificationStatus.APPROVED;
        reactivate();
    }

    public void rejectVerification() {
        this.verificationStatus = VerificationStatus.REJECTED;
        touchUpdatedAt();
    }

    public void requestReReview(String note) {
        if (note != null && !note.isBlank()) {
            this.requestNote = note.trim();
        }
        this.verificationStatus = VerificationStatus.PENDING;
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
        return isActive() && getVerificationStatus() == VerificationStatus.APPROVED;
    }

    @Override
    public boolean canOperateOnPlatform() {
        return isVerifiedAndActive();
    }
}
