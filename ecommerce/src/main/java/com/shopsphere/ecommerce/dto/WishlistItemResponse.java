package com.shopsphere.ecommerce.dto;

public class WishlistItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private double price;

    public WishlistItemResponse() {
    }

    public WishlistItemResponse(Long id, Long productId,
                                String productName, double price) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
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
}