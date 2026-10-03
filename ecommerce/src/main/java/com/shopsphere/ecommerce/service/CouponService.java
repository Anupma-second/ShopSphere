package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Coupon;
import com.shopsphere.ecommerce.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon createCoupon(Coupon coupon) {

        if (couponRepository.existsByCode(coupon.getCode())) {
            throw new RuntimeException(
                    "Coupon already exists with code: " + coupon.getCode());
        }

        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(code);
    }

    public Coupon updateCoupon(Long id, Coupon updatedCoupon) {

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Coupon not found with id: " + id));

        coupon.setCode(updatedCoupon.getCode());
        coupon.setDiscountPercentage(
                updatedCoupon.getDiscountPercentage());
        coupon.setActive(updatedCoupon.isActive());

        return couponRepository.save(coupon);
    }

    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }
}
