package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.AdminStatsResponse;
import com.shopsphere.ecommerce.service.AdminStatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    /** Dashboard numbers: users, orders by status, revenue, 30-day chart, top products. */
    @GetMapping
    public AdminStatsResponse stats() {
        return adminStatsService.getStats();
    }
}
