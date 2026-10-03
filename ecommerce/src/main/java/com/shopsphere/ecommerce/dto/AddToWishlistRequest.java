package com.shopsphere.ecommerce.dto;

public class AddToWishlistRequest {

    private Long productId;

    public AddToWishlistRequest() {
    }

    public AddToWishlistRequest(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
