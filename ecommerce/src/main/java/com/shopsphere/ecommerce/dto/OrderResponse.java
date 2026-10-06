package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private double totalAmount;
    private OrderStatus status;
    private LocalDateTime orderDate;
    private List<OrderItemResponse> items;
    private AddressResponse address;

    // ---- NEW ----
    private PaymentInfoResponse payment;       // null if no payment started
    private List<OrderEventResponse> timeline; // null in the order LIST (kept light)
    private String carrier;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private boolean canCancel;
    private boolean canPay;

    // coupon (discountAmount 0 and couponCode null when none was used)
    private double subtotalAmount;
    private double discountAmount;
    private String couponCode;

    public OrderResponse() {
    }

    public OrderResponse(Long id, double totalAmount,
                         OrderStatus status, LocalDateTime orderDate,
                         List<OrderItemResponse> items,
                         AddressResponse address) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.items = items;
        this.address = address;
    }

    public Long getId() { return id; }
    public double getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public List<OrderItemResponse> getItems() { return items; }
    public AddressResponse getAddress() { return address; }

    public PaymentInfoResponse getPayment() { return payment; }
    public void setPayment(PaymentInfoResponse payment) { this.payment = payment; }

    public List<OrderEventResponse> getTimeline() { return timeline; }
    public void setTimeline(List<OrderEventResponse> timeline) { this.timeline = timeline; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isCanCancel() { return canCancel; }
    public void setCanCancel(boolean canCancel) { this.canCancel = canCancel; }

    public boolean isCanPay() { return canPay; }
    public void setCanPay(boolean canPay) { this.canPay = canPay; }


    public double getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(double subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
