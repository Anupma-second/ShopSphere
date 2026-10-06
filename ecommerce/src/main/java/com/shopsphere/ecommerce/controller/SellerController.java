package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.PageResponse;
import com.shopsphere.ecommerce.dto.SellerOrderResponse;
import com.shopsphere.ecommerce.dto.SellerStatsResponse;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.ProductService;
import com.shopsphere.ecommerce.service.SellerService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Seller dashboard API. Everything is scoped to the logged-in seller.
 * Creating / editing / deleting products still goes through /api/products
 * (ownership is enforced there).
 */
@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;

    public SellerController(SellerService sellerService, ProductService productService) {
        this.sellerService = sellerService;
        this.productService = productService;
    }

    @GetMapping("/stats")
    public SellerStatsResponse stats(@AuthenticationPrincipal User seller) {
        return sellerService.getStats(seller);
    }

    // ---- Products / inventory ----

    @GetMapping("/products")
    public PageResponse<Product> myProducts(
            @AuthenticationPrincipal User seller,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.of(productService.getSellerProducts(seller, pageable));
    }

    @GetMapping("/products/low-stock")
    public List<Product> lowStock(
            @AuthenticationPrincipal User seller,
            @RequestParam(defaultValue = "" + SellerService.DEFAULT_LOW_STOCK) int threshold) {
        return productService.getLowStock(seller, threshold);
    }

    /** Quick inventory update without sending the whole product. */
    @PatchMapping("/products/{id}/stock")
    public Product updateStock(
            @AuthenticationPrincipal User seller,
            @PathVariable Long id,
            @RequestParam int stock) {
        return productService.updateStock(id, stock, seller);
    }

    // ---- Orders ----

    @GetMapping("/orders")
    public List<SellerOrderResponse> orders(
            @AuthenticationPrincipal User seller,
            @RequestParam(required = false) OrderStatus status) {
        return sellerService.getOrders(seller, status);
    }

    @GetMapping("/orders/{id}")
    public SellerOrderResponse order(
            @AuthenticationPrincipal User seller,
            @PathVariable Long id) {
        return sellerService.getOrder(seller, id);
    }

    /** SHIPPED (optional carrier + trackingNumber) or DELIVERED. */
    @PutMapping("/orders/{id}/status")
    public SellerOrderResponse updateStatus(
            @AuthenticationPrincipal User seller,
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String trackingNumber) {
        return sellerService.updateOrderStatus(seller, id, status, carrier, trackingNumber);
    }
}