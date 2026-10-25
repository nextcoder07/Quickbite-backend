package com.quickbite.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountIdentityRepository extends JpaRepository<AccountIdentity, Long> {
    Optional<AccountIdentity> findByFirebaseUid(String firebaseUid);
    Optional<AccountIdentity> findByEmailIgnoreCase(String email);
    boolean existsByFirebaseUid(String firebaseUid);
    boolean existsByEmailIgnoreCase(String email);
}
