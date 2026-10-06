package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(String status);

    // Row locks: the browser "verify" call and Razorpay's webhook can arrive
    // at the same moment - the second one waits and then sees the final state.
    // (must be called inside a @Transactional method)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.razorpayOrderId = :rid")
    Optional<Payment> lockByRazorpayOrderId(@Param("rid") String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.order.id = :oid")
    Optional<Payment> lockByOrderId(@Param("oid") Long orderId);
}
