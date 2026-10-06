package com.shopsphere.ecommerce.dto;

public class CheckoutRequest {

    private Long addressId;

    private String couponCode;   // optional

    public CheckoutRequest() {
    }

    public CheckoutRequest(Long addressId) {
        this.addressId = addressId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}