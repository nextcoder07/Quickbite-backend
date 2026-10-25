package com.quickbite.shop;

import com.quickbite.common.enums.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByOwnerId(Long ownerId);
    Optional<Shop> findByOwner(ShopOwner owner);
    List<Shop> findByApprovalStatusAndDeactivatedFalseAndOpenTrue(ShopStatus status);
    List<Shop> findByApprovalStatus(ShopStatus status);
    List<Shop> findAllByDeactivatedFalse();
}
