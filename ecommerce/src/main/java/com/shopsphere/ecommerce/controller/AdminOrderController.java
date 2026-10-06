package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.OrderResponse;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.service.OrderService;
import com.shopsphere.ecommerce.service.RefundService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final RefundService refundService;

    public AdminOrderController(OrderService orderService, RefundService refundService) {
        this.orderService = orderService;
        this.refundService = refundService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderAdmin(id);
    }

    /**
     * SHIPPED accepts optional carrier + trackingNumber.
     * CANCELLED on a paid order triggers the refund.
     */
    @PutMapping("/{id}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String trackingNumber) {

        orderService.updateStatusAsAdmin(id, status, carrier, trackingNumber);

        if (status == OrderStatus.CANCELLED) {
            refundService.processRefundForOrder(id);
        }

        return orderService.getOrderAdmin(id);
    }

    /** Manually retry a refund that kept failing. */
    @PostMapping("/{id}/refund")
    public OrderResponse retryRefund(@PathVariable Long id) {
        refundService.retryRefund(id);
        return orderService.getOrderAdmin(id);
    }
}
