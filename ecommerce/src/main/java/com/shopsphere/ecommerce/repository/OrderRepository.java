package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findByStatusAndOrderDateBefore(
            OrderStatus status, LocalDateTime before);

    List<Order> findAllByOrderByOrderDateDesc();


    /** "Once per customer" coupons: has this user already used the code? */
    boolean existsByUserIdAndCouponCodeAndStatusNot(Long userId, String couponCode, OrderStatus status);

    // ---- Admin stats ----

    /** Rows: [OrderStatus, count] */
    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countGroupedByStatus();

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status in :statuses")
    Double sumRevenue(@Param("statuses") Collection<OrderStatus> statuses);

    /** Rows: [orderDate, totalAmount] - grouped per day in Java so it works on any DB. */
    @Query("select o.orderDate, o.totalAmount from Order o "
            + "where o.status in :statuses and o.orderDate >= :since")
    List<Object[]> revenueRowsSince(@Param("statuses") Collection<OrderStatus> statuses,
                                    @Param("since") LocalDateTime since);
}