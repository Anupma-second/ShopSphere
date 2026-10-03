package com.shopsphere.ecommerce.dto;

public class CreatePaymentRequest {

    private Long orderId;

    public CreatePaymentRequest() {
    }

    public CreatePaymentRequest(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
