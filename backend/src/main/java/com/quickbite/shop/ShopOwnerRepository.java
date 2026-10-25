package com.quickbite.shop;

import com.quickbite.identity.AccountIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShopOwnerRepository extends JpaRepository<ShopOwner, Long> {
    Optional<ShopOwner> findByIdentity(AccountIdentity identity);
    Optional<ShopOwner> findByIdentityId(Long identityId);
    boolean existsByIdentityId(Long identityId);
}
