package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.CheckoutRequest;
import com.shopsphere.ecommerce.dto.OrderResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FIX: removed POST /, PUT /{id} and DELETE /{id}. They accepted raw Order
 * entities, so any customer could create, rewrite or delete orders.
 * Orders are created only through /checkout.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal User user) {
        return orderService.getOrdersByUser(user);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return orderService.getOrderForUser(id, user);
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User user) {
        return orderService.checkout(request, user);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        orderService.cancelOrder(id, user);
        return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
    }
}
