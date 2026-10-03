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

    public Long getId() {
        return id;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public AddressResponse getAddress() {
        return address;
    }
}
