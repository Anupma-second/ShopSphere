package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.ReviewRequest;
import com.shopsphere.ecommerce.dto.ReviewResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // logged in
    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(user, request));
    }

    // public (see SecurityConfig)
    @GetMapping("/product/{productId}")
    public List<ReviewResponse> forProduct(@PathVariable Long productId) {
        return reviewService.listForProduct(productId);
    }

    // owner or admin
    @PutMapping("/{id}")
    public ReviewResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        reviewService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
