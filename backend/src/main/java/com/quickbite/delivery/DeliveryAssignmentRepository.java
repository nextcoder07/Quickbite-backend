package com.quickbite.delivery;

import com.quickbite.common.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    List<DeliveryAssignment> findByDeliveryPartnerIdOrderByAssignedAtDesc(Long deliveryPartnerId);
    List<DeliveryAssignment> findByStatus(DeliveryStatus status);
}
