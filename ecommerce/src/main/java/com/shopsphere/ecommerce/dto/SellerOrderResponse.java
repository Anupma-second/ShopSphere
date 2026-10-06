package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An order as a seller sees it: only the seller's own lines and subtotal.
 * canUpdateStatus is false when the order also contains items from the
 * store or another seller - those orders are shipped by the admin.
 */
public record SellerOrderResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime orderDate,
        String customerName,
        AddressResponse shippingAddress,
        List<OrderItemResponse> items,
        double sellerSubtotal,
        String carrier,
        String trackingNumber,
        boolean canUpdateStatus) {
}
