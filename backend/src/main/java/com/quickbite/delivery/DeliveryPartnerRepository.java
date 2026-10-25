package com.quickbite.delivery;

import com.quickbite.identity.AccountIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    Optional<DeliveryPartner> findByIdentity(AccountIdentity identity);
    Optional<DeliveryPartner> findByIdentityId(Long identityId);
    boolean existsByIdentityId(Long identityId);
}
