package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.Review;

import java.time.LocalDateTime;

public class ReviewResponse {

    private Long id;
    private Long productId;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public ReviewResponse() {
    }

    public static ReviewResponse from(Review r) {
        ReviewResponse res = new ReviewResponse();
        res.id = r.getId();
        res.productId = r.getProduct().getId();
        res.userName = r.getUser().getName();
        res.rating = r.getRating();
        res.comment = r.getComment();
        res.createdAt = r.getCreatedAt();
        return res;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getUserName() { return userName; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
