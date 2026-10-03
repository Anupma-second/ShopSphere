package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.entity.ProductImage;
import com.shopsphere.ecommerce.service.ProductImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-images")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @PostMapping
    public ProductImage createImage(@RequestBody ProductImage productImage) {
        return productImageService.createImage(productImage);
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
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        productImageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}