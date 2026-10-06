package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.Product;

public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private double price;
    private int quantity;
    private double totalPrice;
    private String imageUrl;   // main photo, null if none
    private int stock;         // so the cart can warn when quantity > stock

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

    /** Fills in the photo and current stock from the product. */
    public CartItemResponse withProduct(Product product) {
        this.imageUrl = product.getImages().isEmpty() ? null : product.getImages().get(0).imageUrl();
        this.stock = product.getStock();
        return this;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public int getStock() {
        return stock;
    }
}