package com.shopsphere.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyCouponRequest(
        @NotBlank(message = "Enter a coupon code")
        String code) {
}