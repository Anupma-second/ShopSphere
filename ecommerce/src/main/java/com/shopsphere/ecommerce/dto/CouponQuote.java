package com.shopsphere.ecommerce.dto;

/** What a coupon would do to the current cart (shown before paying). */
public record CouponQuote(
        String code,
        String summary,       // e.g. "10% off, up to ₹200"
        double subtotal,
        double discount,
        double total) {
}