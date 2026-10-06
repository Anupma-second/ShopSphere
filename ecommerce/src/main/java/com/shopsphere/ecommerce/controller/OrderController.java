package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.ApplyCouponRequest;
import com.shopsphere.ecommerce.dto.CheckoutRequest;
import com.shopsphere.ecommerce.dto.CouponQuote;
import com.shopsphere.ecommerce.dto.OrderResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.OrderService;
import com.shopsphere.ecommerce.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final RefundService refundService;

    public OrderController(OrderService orderService, RefundService refundService) {
        this.orderService = orderService;
        this.refundService = refundService;
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


    /** Checks a coupon against the current cart without placing an order. */
    @PostMapping("/apply-coupon")
    public CouponQuote applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            @AuthenticationPrincipal User user) {
        return orderService.previewCoupon(request.code(), user);
    }

    /**
     * Cancels the order (committed first), then - if it was already paid -
     * asks Razorpay for the refund. Returns the fresh order so the page shows
     * the real refund state. If Razorpay is down the refund simply stays
     * REFUND_REQUIRED and the scheduler retries it.
     */
    @PutMapping("/{id}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        orderService.cancelOrder(id, user);
        refundService.processRefundForOrder(id);

        return orderService.getOrderForUser(id, user);
    }
}
