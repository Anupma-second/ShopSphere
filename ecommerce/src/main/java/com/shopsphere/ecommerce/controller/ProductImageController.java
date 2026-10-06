package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.entity.ProductImage;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.ProductImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-images")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    // owner seller or admin
    @PostMapping
    public ProductImage createImage(
            @AuthenticationPrincipal User user,
            @RequestBody ProductImage productImage) {
        return productImageService.createImage(productImage, user);
    }

    @GetMapping
    public List<ProductImage> getAllImages() {
        return productImageService.getAllImages();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductImage> getImageById(@PathVariable Long id) {
        return productImageService.getImageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        productImageService.deleteImage(id, user);
        return ResponseEntity.noContent().build();
    }
}