package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.Payment;

import java.time.LocalDateTime;

/** Payment details shown on the order page (never exposes keys or signatures). */
public class PaymentInfoResponse {

    private String status;
    private String method;
    private double amount;
    private String razorpayPaymentId;
    private LocalDateTime paidAt;
    private String refundId;
    private LocalDateTime refundedAt;
    private String failureReason;

    public PaymentInfoResponse() {
    }

    public static PaymentInfoResponse from(Payment p) {
        PaymentInfoResponse r = new PaymentInfoResponse();
        r.status = p.getStatus();
        r.method = p.getMethod();
        r.amount = p.getAmount();
        r.razorpayPaymentId = p.getRazorpayPaymentId();
        r.paidAt = p.getPaidAt();
        r.refundId = p.getRazorpayRefundId();
        r.refundedAt = p.getRefundedAt();
        r.failureReason = p.getFailureReason();
        return r;
    }

    public String getStatus() { return status; }
    public String getMethod() { return method; }
    public double getAmount() { return amount; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getRefundId() { return refundId; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public String getFailureReason() { return failureReason; }
}
