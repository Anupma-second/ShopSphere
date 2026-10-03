package com.shopsphere.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases stock held by orders that were created at checkout but never
 * paid (user closed the tab, payment failed, etc.).
 */
@Component
public class OrderCleanupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OrderCleanupScheduler.class);

    private final OrderService orderService;

    @Value("${app.order.pending-timeout-minutes:30}")
    private int timeoutMinutes;

    public OrderCleanupScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)   // every 5 min
    public void cancelUnpaidOrders() {
        int cancelled = orderService.cancelExpiredPendingOrders(timeoutMinutes);
        if (cancelled > 0) {
            log.info("Cancelled {} unpaid order(s) and restored their stock", cancelled);
        }
    }
}
