package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Takes one use of the coupon, but only while it is under its limit.
     * Returns 1 if a use was taken, 0 if the limit was already reached -
     * so two customers can never both get the last use.
     */
    @Modifying
    @Query("update Coupon c set c.usedCount = coalesce(c.usedCount, 0) + 1 "
            + "where c.id = :id and (c.usageLimit is null or coalesce(c.usedCount, 0) < c.usageLimit)")
    int reserveUse(@Param("id") Long id);

    /** Gives a use back (order cancelled). */
    @Modifying
    @Query("update Coupon c set c.usedCount = coalesce(c.usedCount, 0) - 1 "
            + "where c.code = :code and coalesce(c.usedCount, 0) > 0")
    int releaseUse(@Param("code") String code);
}