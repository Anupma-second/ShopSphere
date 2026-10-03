package com.shopsphere.ecommerce.dto;

public class OrderItemResponse {

    private Long productId;
    private String productName;
    private int quantity;
    private double price;

    public OrderItemResponse() {
    }

    public OrderItemResponse(Long productId, String productName,
                             int quantity, double price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
