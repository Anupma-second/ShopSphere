package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.PageResponse;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // seller or admin (see SecurityConfig); sellers become the owner
    @PostMapping
    public Product createProduct(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody Product product) {
        return productService.createProduct(product, user);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * Public, paged catalogue search. Example:
     * /api/products/search?q=shirt&categoryId=2&minPrice=100&maxPrice=999
     *     &inStock=true&page=0&size=12&sort=price,asc
     */
    @GetMapping("/search")
    public PageResponse<Product> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @PageableDefault(size = 12, sort = "id") Pageable pageable) {

        return PageResponse.of(productService.search(
                q, categoryId, minPrice, maxPrice, inStock, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // owner seller or admin
    @PutMapping("/{id}")
    public Product updateProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody Product product) {

        return productService.updateProduct(id, product, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        productService.deleteProduct(id, user);
        return ResponseEntity.noContent().build();
    }
}