package com.shopsphere.ecommerce.dto;

import java.util.List;

/** Revenue / units count only paid orders (CONFIRMED, SHIPPED, DELIVERED). */
public record SellerStatsResponse(
        long totalProducts,
        long lowStockProducts,
        long paidOrders,
        long ordersToShip,
        long unitsSold,
        double revenue,
        List<TopProductResponse> topProducts) {
}
