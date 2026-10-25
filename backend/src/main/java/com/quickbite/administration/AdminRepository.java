package com.quickbite.administration;

import com.quickbite.identity.AccountIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByIdentity(AccountIdentity identity);
    Optional<Admin> findByIdentityId(Long identityId);
    boolean existsByIdentityId(Long identityId);
}
