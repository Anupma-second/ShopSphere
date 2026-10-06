package com.shopsphere.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Retries refunds that failed or were flagged while nobody was watching. */
@Component
public class RefundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefundScheduler.class);

    private final RefundService refundService;

    public RefundScheduler(RefundService refundService) {
        this.refundService = refundService;
    }

    @Scheduled(fixedDelay = 120_000, initialDelay = 45_000)   // every 2 min
    public void retryPendingRefunds() {
        for (Long orderId : refundService.findPendingOrderIds()) {
            try {
                refundService.processRefundForOrder(orderId);
            } catch (Exception e) {
                log.warn("Refund retry for order {} failed: {}", orderId, e.getMessage());
            }
        }
    }
}
