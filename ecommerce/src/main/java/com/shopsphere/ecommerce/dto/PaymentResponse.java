package com.shopsphere.ecommerce.dto;

public class PaymentResponse {

    private String razorpayOrderId;
    private Long orderId;
    private double amount;
    private String status;

    public PaymentResponse() {
    }

    public PaymentResponse(
            String razorpayOrderId,
            Long orderId,
            double amount,
            String status) {

        this.razorpayOrderId = razorpayOrderId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}
