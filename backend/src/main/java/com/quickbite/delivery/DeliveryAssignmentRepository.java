package com.quickbite.delivery;

import com.quickbite.common.enums.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    List<DeliveryAssignment> findByDeliveryPartnerIdOrderByAssignedAtDesc(Long deliveryPartnerId);
    List<DeliveryAssignment> findByStatus(DeliveryStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT da FROM DeliveryAssignment da WHERE da.id = :id")
    Optional<DeliveryAssignment> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DeliveryAssignment da
            SET da.deliveryPartner = :partner,
                da.status = :acceptedStatus,
                da.assignedAt = :now,
                da.acceptedAt = :now,
                da.updatedAt = :now,
                da.version = da.version + 1
            WHERE da.id = :id
              AND da.status = :unassignedStatus
              AND da.deliveryPartner IS NULL
            """)
    int claimAtomically(
            @Param("id") Long id,
            @Param("partner") DeliveryPartner partner,
            @Param("unassignedStatus") DeliveryStatus unassignedStatus,
            @Param("acceptedStatus") DeliveryStatus acceptedStatus,
            @Param("now") Instant now
    );
}
