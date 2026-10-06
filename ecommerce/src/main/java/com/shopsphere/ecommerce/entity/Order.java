package com.shopsphere.ecommerce.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double totalAmount;      // what the customer pays (after any coupon)

    // ---- NEW: coupon (all null when no coupon was used) ----
    private Double subtotalAmount;   // items total before the discount
    private Double discountAmount;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Kept for reference; the delivery details below are a COPY taken at
    // checkout, so editing/deleting an address never changes old orders.
    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    // ---- NEW: delivery address snapshot (nullable for old orders) ----
    private String shipFullName;
    private String shipPhone;
    private String shipAddressLine;
    private String shipCity;
    private String shipState;
    private String shipPostalCode;
    private String shipCountry;

    // ---- NEW: tracking ----
    private String carrier;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(Double totalAmount, OrderStatus status,
                 LocalDateTime orderDate, User user, Address address) {
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.user = user;
        this.address = address;
    }

    /** Copies the address fields into the order (call once at checkout). */
    public void snapshotAddress(Address a) {
        this.address = a;
        this.shipFullName = a.getFullName();
        this.shipPhone = a.getPhone();
        this.shipAddressLine = a.getAddressLine();
        this.shipCity = a.getCity();
        this.shipState = a.getState();
        this.shipPostalCode = a.getPostalCode();
        this.shipCountry = a.getCountry();
    }

    public Long getId() { return id; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }


    public Double getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(Double subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public String getShipFullName() { return shipFullName; }
    public String getShipPhone() { return shipPhone; }
    public String getShipAddressLine() { return shipAddressLine; }
    public String getShipCity() { return shipCity; }
    public String getShipState() { return shipState; }
    public String getShipPostalCode() { return shipPostalCode; }
    public String getShipCountry() { return shipCountry; }

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

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
