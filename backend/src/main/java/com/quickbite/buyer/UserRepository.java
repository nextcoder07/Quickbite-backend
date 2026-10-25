package com.quickbite.buyer;

import com.quickbite.identity.AccountIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdentity(AccountIdentity identity);
    Optional<User> findByIdentityId(Long identityId);
    boolean existsByIdentityId(Long identityId);
}
