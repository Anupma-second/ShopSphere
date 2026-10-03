package com.shopsphere.ecommerce.dto;

public class CheckoutRequest {

    private Long addressId;

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
}
