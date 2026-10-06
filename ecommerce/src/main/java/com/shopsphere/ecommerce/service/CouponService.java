package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.CouponQuote;
import com.shopsphere.ecommerce.entity.Coupon;
import com.shopsphere.ecommerce.entity.Coupon.DiscountType;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ConflictException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.CouponRepository;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CouponService {

    // Razorpay can't take a ₹0 payment, so a coupon never brings the total below this
    private static final double MIN_PAYABLE = 1.0;

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    public CouponService(CouponRepository couponRepository, OrderRepository orderRepository) {
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
    }

    /** The coupon picked at checkout plus what it takes off. */
    public record Applied(Coupon coupon, double discount) {
    }

    // ---------- Shopper side ----------

    /** What this code would do to a cart of the given value. Throws a readable 400 if it can't be used. */
    public CouponQuote quote(String code, double subtotal, User user) {
        Applied applied = check(code, subtotal, user);
        double discount = applied.discount();
        return new CouponQuote(
                applied.coupon().getCode(),
                summary(applied.coupon()),
                round(subtotal),
                discount,
                round(subtotal - discount));
    }

    /**
     * Checks the code and takes one use of it. Call inside the checkout
     * transaction: if checkout fails later, the use is rolled back too.
     */
    @Transactional
    public Applied redeem(String code, double subtotal, User user) {
        Applied applied = check(code, subtotal, user);
        if (couponRepository.reserveUse(applied.coupon().getId()) == 0) {
            throw new BadRequestException("This coupon has reached its usage limit");
        }
        return applied;
    }

    /** Order cancelled - the coupon can be used again. */
    @Transactional
    public void release(String code) {
        if (code != null) {
            couponRepository.releaseUse(code);
        }
    }

    private Applied check(String rawCode, double subtotal, User user) {
        String code = normalize(rawCode);

        Coupon c = couponRepository.findByCode(code)
                .filter(Coupon::isActive)
                .orElseThrow(() -> new BadRequestException("“" + code + "” is not a valid coupon code"));

        if (c.isExpired()) {
            throw new BadRequestException("This coupon has expired");
        }
        if (c.isUsedUp()) {
            throw new BadRequestException("This coupon has reached its usage limit");
        }
        if (c.getMinOrderAmount() != null && subtotal < c.getMinOrderAmount()) {
            throw new BadRequestException("Add " + Money.inr(c.getMinOrderAmount() - subtotal)
                    + " more to use this coupon (minimum order " + Money.inr(c.getMinOrderAmount()) + ")");
        }
        if (c.isOncePerCustomer() && orderRepository
                .existsByUserIdAndCouponCodeAndStatusNot(user.getId(), c.getCode(), OrderStatus.CANCELLED)) {
            throw new BadRequestException("You've already used this coupon");
        }

        double discount = c.getDiscountType() == DiscountType.FIXED
                ? c.getFlatAmount()
                : subtotal * c.getDiscountPercentage() / 100.0;

        if (c.getDiscountType() == DiscountType.PERCENT && c.getMaxDiscount() != null) {
            discount = Math.min(discount, c.getMaxDiscount());
        }
        discount = round(Math.max(0, Math.min(discount, subtotal - MIN_PAYABLE)));

        return new Applied(c, discount);
    }

    // ---------- Admin side ----------

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(normalize(code));
    }

    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(normalize(coupon.getCode()));
        validate(coupon);

        if (couponRepository.existsByCode(coupon.getCode())) {
            throw new ConflictException("A coupon with code " + coupon.getCode() + " already exists");
        }
        return couponRepository.save(coupon);
    }

    public Coupon updateCoupon(Long id, Coupon updated) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));

        String code = normalize(updated.getCode());
        if (!code.equals(coupon.getCode()) && couponRepository.existsByCode(code)) {
            throw new ConflictException("A coupon with code " + code + " already exists");
        }

        coupon.setCode(code);
        coupon.setDiscountType(updated.getDiscountType());
        coupon.setDiscountPercentage(updated.getDiscountPercentage());
        coupon.setMaxDiscount(updated.getMaxDiscount());
        coupon.setFlatAmount(updated.getFlatAmount());
        coupon.setMinOrderAmount(updated.getMinOrderAmount());
        coupon.setExpiresAt(updated.getExpiresAt());
        coupon.setUsageLimit(updated.getUsageLimit());
        coupon.setOncePerCustomer(updated.isOncePerCustomer());
        coupon.setActive(updated.isActive());
        // usedCount is never taken from the request

        validate(coupon);
        return couponRepository.save(coupon);
    }

    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    private void validate(Coupon c) {
        if (!c.getCode().matches("[A-Z0-9_-]{3,30}")) {
            throw new BadRequestException("Code must be 3-30 letters, numbers, - or _");
        }
        if (c.getDiscountType() == DiscountType.FIXED) {
            if (c.getFlatAmount() == null || c.getFlatAmount() <= 0) {
                throw new BadRequestException("Enter the ₹ amount to take off");
            }
        } else {
            Double pct = c.getDiscountPercentage();
            if (pct == null || pct <= 0 || pct > 90) {
                throw new BadRequestException("Percentage must be between 1 and 90");
            }
            if (c.getMaxDiscount() != null && c.getMaxDiscount() <= 0) {
                throw new BadRequestException("Maximum discount must be more than ₹0");
            }
        }
        if (c.getMinOrderAmount() != null && c.getMinOrderAmount() < 0) {
            throw new BadRequestException("Minimum order can't be negative");
        }
        if (c.getUsageLimit() != null && c.getUsageLimit() < 1) {
            throw new BadRequestException("Usage limit must be at least 1 (or leave it empty)");
        }
    }

    // ---------- helpers ----------

    /** e.g. "10% off, up to ₹200 · min order ₹999" */
    public static String summary(Coupon c) {
        StringBuilder s = new StringBuilder();
        if (c.getDiscountType() == DiscountType.FIXED) {
            s.append(Money.inr(c.getFlatAmount())).append(" off");
        } else {
            s.append(trim(c.getDiscountPercentage())).append("% off");
            if (c.getMaxDiscount() != null) {
                s.append(", up to ").append(Money.inr(c.getMaxDiscount()));
            }
        }
        if (c.getMinOrderAmount() != null && c.getMinOrderAmount() > 0) {
            s.append(" · min order ").append(Money.inr(c.getMinOrderAmount()));
        }
        return s.toString();
    }

    private static String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Enter a coupon code");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(Double d) {
        return d == Math.floor(d) ? String.valueOf(d.longValue()) : String.valueOf(d);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}