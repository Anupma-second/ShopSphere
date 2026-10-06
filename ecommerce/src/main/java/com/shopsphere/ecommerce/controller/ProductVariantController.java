package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.entity.ProductVariant;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.ProductVariantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-variants")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    // owner seller or admin
    @PostMapping
    public ProductVariant createVariant(
            @AuthenticationPrincipal User user,
            @RequestBody ProductVariant variant) {

        return productVariantService.createVariant(variant, user);
    }

    @GetMapping
    public List<ProductVariant> getAllVariants() {
        return productVariantService.getAllVariants();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariant> getVariantById(
            @PathVariable Long id) {

        return productVariantService.getVariantById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ProductVariant updateVariant(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody ProductVariant variant) {

        return productVariantService.updateVariant(id, variant, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariant(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        productVariantService.deleteVariant(id, user);

        return ResponseEntity.noContent().build();
    }
}