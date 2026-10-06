package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Revenue counts only paid orders (CONFIRMED, SHIPPED, DELIVERED). */
public record AdminStatsResponse(
        Map<Role, Long> usersByRole,
        Map<OrderStatus, Long> ordersByStatus,
        long totalProducts,
        long lowStockProducts,
        double totalRevenue,
        double revenueLast30Days,
        List<DailyRevenue> dailyRevenueLast30Days,
        List<TopProductResponse> topProducts) {

    public record DailyRevenue(LocalDate date, double revenue, long orders) {
    }
}
