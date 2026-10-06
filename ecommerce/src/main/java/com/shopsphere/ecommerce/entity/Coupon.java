package com.shopsphere.ecommerce.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A discount code. Every rule except the discount itself is optional
 * (null = no limit), so coupons created before these fields existed keep
 * working as plain "x% off" codes.
 */
@Entity
@Table(name = "coupons")
public class Coupon extends Auditable {

    public enum DiscountType { PERCENT, FIXED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;              // always stored in UPPER CASE

    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // null (old rows) = PERCENT

    private Double discountPercentage; // PERCENT: e.g. 10 = 10% off
    private Double maxDiscount;        // PERCENT: optional cap in ₹
    private Double flatAmount;         // FIXED: ₹ off

    private Double minOrderAmount;     // optional minimum cart value in ₹
    private LocalDateTime expiresAt;   // optional
    private Integer usageLimit;        // optional total number of uses
    private Integer usedCount = 0;     // maintained by the server only
    private Boolean oncePerCustomer = false;

    private boolean active = true;

    // Only a no-args constructor on purpose: Jackson then fills fields through
    // the setters, so anything left out of the JSON keeps its default.
    public Coupon() {
    }

    /** PERCENT unless explicitly FIXED. */
    public DiscountType getDiscountType() {
        return discountType == null ? DiscountType.PERCENT : discountType;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsedUp() {
        return usageLimit != null && getUsedCount() >= usageLimit;
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public Double getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Double maxDiscount) { this.maxDiscount = maxDiscount; }

    public Double getFlatAmount() { return flatAmount; }
    public void setFlatAmount(Double flatAmount) { this.flatAmount = flatAmount; }

    public Double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount == null ? 0 : usedCount; }

    public boolean isOncePerCustomer() { return Boolean.TRUE.equals(oncePerCustomer); }
    public void setOncePerCustomer(boolean oncePerCustomer) { this.oncePerCustomer = oncePerCustomer; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}