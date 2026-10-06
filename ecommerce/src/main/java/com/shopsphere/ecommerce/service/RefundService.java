package com.shopsphere.ecommerce.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.shopsphere.ecommerce.entity.OrderEvent;
import com.shopsphere.ecommerce.entity.Payment;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import com.shopsphere.ecommerce.util.Money;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends refunds to Razorpay for payments flagged REFUND_REQUIRED.
 * Never throws to the caller: a failed attempt just stays REFUND_REQUIRED
 * and is retried by RefundScheduler (up to app.refund.max-attempts times).
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository paymentRepository;
    private final OrderEventService eventService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${app.refund.max-attempts:5}")
    private int maxAttempts;

    public RefundService(PaymentRepository paymentRepository,
                         OrderEventService eventService) {
        this.paymentRepository = paymentRepository;
        this.eventService = eventService;
    }

    /** Order ids whose refund still has to be sent (used by the scheduler). */
    @Transactional(readOnly = true)
    public List<Long> findPendingOrderIds() {
        return paymentRepository.findByStatus(PaymentStatus.REFUND_REQUIRED)
                .stream()
                .filter(p -> p.getRazorpayPaymentId() != null)
                .filter(p -> p.getRefundAttempts() < maxAttempts)
                .map(p -> p.getOrder().getId())
                .toList();
    }

    @Transactional
    public void processRefundForOrder(Long orderId) {
        paymentRepository.lockByOrderId(orderId).ifPresent(this::processLocked);
    }

    /** Admin "retry": resets the attempt counter and tries again now. */
    @Transactional
    public void retryRefund(Long orderId) {

        Payment payment = paymentRepository.lockByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!PaymentStatus.REFUND_REQUIRED.equals(payment.getStatus())) {
            throw new BadRequestException(
                    "No refund is pending for this order (payment is "
                            + payment.getStatus() + ")");
        }

        payment.setRefundAttempts(0);
        processLocked(payment);
    }

    private void processLocked(Payment payment) {

        if (!PaymentStatus.REFUND_REQUIRED.equals(payment.getStatus())) {
            return;                                   // nothing to do / already done
        }

        if (payment.getRazorpayPaymentId() == null) {
            log.warn("Payment {} needs a refund but has no Razorpay payment id",
                    payment.getId());
            return;
        }

        if (payment.getRefundAttempts() >= maxAttempts) {
            return;                                   // an admin must retry manually
        }

        payment.setRefundAttempts(payment.getRefundAttempts() + 1);

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", Math.round(payment.getAmount() * 100));   // paise

            Refund refund = client.payments.refund(
                    payment.getRazorpayPaymentId(), request);

            String refundId = refund.<String>get("id");
            String refundStatus = refund.has("status") ? refund.<String>get("status") : "pending";

            payment.setRazorpayRefundId(refundId);

            if ("processed".equals(refundStatus)) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
                eventService.record(payment.getOrder(), OrderEvent.REFUND_COMPLETED,
                        "Refund of " + Money.inr(payment.getAmount())
                                + " completed to your original payment method.");
            } else {
                // Razorpay is still processing it; the refund.processed
                // webhook (or a manual check) moves it to REFUNDED
                payment.setStatus(PaymentStatus.REFUND_INITIATED);
                eventService.record(payment.getOrder(), OrderEvent.REFUND_INITIATED,
                        "Refund of " + Money.inr(payment.getAmount())
                                + " has been sent to your bank. It usually takes 5-7 business days.");
            }

        } catch (Exception e) {
            log.warn("Refund attempt {} for payment {} failed: {}",
                    payment.getRefundAttempts(), payment.getId(), e.getMessage());
            // stays REFUND_REQUIRED -> retried by the scheduler
        }

        paymentRepository.save(payment);
    }
}
