package com.quickbite.payment;

import com.quickbite.common.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPaymentId(Long paymentId);
    Optional<Refund> findByIdempotencyKey(String idempotencyKey);
    List<Refund> findByStatus(RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.id = :paymentId AND r.status IN ('APPROVED', 'PROCESSED', 'PROCESSING')")
    BigDecimal sumEligibleRefundAmountByPaymentId(@Param("paymentId") Long paymentId);
}
