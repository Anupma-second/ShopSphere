package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.OrderResponse;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FIX: PUT /{id}/status had no role check, so any logged-in customer could
 * mark any order SHIPPED/DELIVERED. The whole controller is now admin only
 * (also enforced by the /api/admin/** rule in SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderAdmin(id);
    }

    @PutMapping("/{id}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return orderService.updateStatusAsAdmin(id, status);
    }
}
