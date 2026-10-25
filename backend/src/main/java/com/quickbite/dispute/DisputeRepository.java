package com.quickbite.dispute;

import com.quickbite.common.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByOrderId(Long orderId);
    List<Dispute> findByRaisedByIdentityId(Long identityId);
    List<Dispute> findByStatus(DisputeStatus status);
}
