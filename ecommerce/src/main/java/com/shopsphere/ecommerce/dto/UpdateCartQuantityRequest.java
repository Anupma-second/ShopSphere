package com.shopsphere.ecommerce.dto;

public class UpdateCartQuantityRequest {

    private Integer quantity;

    public UpdateCartQuantityRequest() {
    }

    public UpdateCartQuantityRequest(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}