package com.quickbite.ordering;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    List<CustomerOrder> findByBuyerIdOrderByPlacedAtDesc(Long buyerId);
    List<CustomerOrder> findByShopIdOrderByPlacedAtDesc(Long shopId);
    List<CustomerOrder> findAllByOrderByPlacedAtDesc();
}
