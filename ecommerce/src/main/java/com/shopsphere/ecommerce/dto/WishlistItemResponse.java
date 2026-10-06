package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.Product;

public class WishlistItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private double price;
    private String imageUrl;   // main photo, null if none
    private int stock;

    public WishlistItemResponse() {
    }

    public WishlistItemResponse(Long id, Long productId,
                                String productName, double price) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
    }

    /** Fills in the photo and current stock from the product. */
    public WishlistItemResponse withProduct(Product product) {
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

    public String getImageUrl() {
        return imageUrl;
    }

    public int getStock() {
        return stock;
    }
}