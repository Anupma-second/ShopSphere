package com.shopsphere.ecommerce.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One line of an order's timeline ("Order placed", "Shipped", "Refunded"...). */
@Entity
@Table(name = "order_events")
public class OrderEvent {

    public static final String ORDER_PLACED = "ORDER_PLACED";
    public static final String PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String REFUND_INITIATED = "REFUND_INITIATED";
    public static final String REFUND_COMPLETED = "REFUND_COMPLETED";
    public static final String REFUND_FAILED = "REFUND_FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private String type;

    @Column(length = 500)
    private String message;

    private LocalDateTime createdAt;

    public OrderEvent() {
    }

    public OrderEvent(Order order, String type, String message) {
        this.order = order;
        this.type = type;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
