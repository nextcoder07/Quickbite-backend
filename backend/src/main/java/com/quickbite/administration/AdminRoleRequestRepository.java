package com.quickbite.administration;

import com.quickbite.common.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminRoleRequestRepository extends JpaRepository<AdminRoleRequest, Long> {
    List<AdminRoleRequest> findByIdentityIdOrderByCreatedAtDesc(Long identityId);
    boolean existsByIdentityIdAndStatus(Long identityId, VerificationStatus status);
    List<AdminRoleRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(VerificationStatus status);
}

