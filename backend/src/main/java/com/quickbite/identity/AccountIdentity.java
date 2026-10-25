package com.quickbite.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "account_identities")
public class AccountIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountIdentity() {}

    public AccountIdentity(String firebaseUid, String email) {
        this(null, firebaseUid, email);
    }

    public AccountIdentity(Long id, String firebaseUid, String email) {
        this.id = id;
        this.firebaseUid = Objects.requireNonNull(firebaseUid, "firebaseUid must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null").toLowerCase().trim();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (email != null) email = email.toLowerCase().trim();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateEmail(String newEmail) {
        if (newEmail != null && !newEmail.isBlank()) {
            this.email = newEmail.toLowerCase().trim();
            this.updatedAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountIdentity that = (AccountIdentity) o;
        return Objects.equals(id, that.id) || Objects.equals(firebaseUid, that.firebaseUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firebaseUid);
    }
}
