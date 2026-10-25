package com.quickbite.accountdeletion;

import com.quickbite.common.enums.DeletionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {
    List<AccountDeletionRequest> findByRequesterIdentityId(Long identityId);
    List<AccountDeletionRequest> findByStatus(DeletionRequestStatus status);
    boolean existsByRequesterIdentityIdAndStatus(Long identityId, DeletionRequestStatus status);
}
