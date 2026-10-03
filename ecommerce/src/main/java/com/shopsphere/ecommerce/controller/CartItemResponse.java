package com.shopsphere.ecommerce.dto;

public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private double price;
    private int quantity;
    private double totalPrice;

    public CartItemResponse() {
    }

    public CartItemResponse(Long id, Long productId, String productName,
                            double price, int quantity, double totalPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}