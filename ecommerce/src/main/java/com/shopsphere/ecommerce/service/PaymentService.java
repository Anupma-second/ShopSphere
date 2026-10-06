package com.shopsphere.ecommerce.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.shopsphere.ecommerce.dto.CreatePaymentRequest;
import com.shopsphere.ecommerce.dto.PaymentResponse;
import com.shopsphere.ecommerce.dto.VerifyPaymentRequest;
import com.shopsphere.ecommerce.entity.OrderEvent;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Payment;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.ApiException;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ConflictException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import com.shopsphere.ecommerce.util.Money;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventService eventService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // set in Razorpay dashboard -> Webhooks (different from the API key secret)
    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderEventService eventService) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.eventService = eventService;
    }

    // =====================================================================
    // 1. Create the Razorpay order
    // =====================================================================

    public PaymentResponse createPayment(
            CreatePaymentRequest request,
            User user) throws Exception {

        com.shopsphere.ecommerce.entity.Order order =
                orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "This order is " + order.getStatus() + " and cannot be paid");
        }

        Payment existingPayment = paymentRepository
                .findByOrderId(order.getId())
                .orElse(null);

        if (existingPayment != null) {

            if (PaymentStatus.PAID.equals(existingPayment.getStatus())) {
                throw new ConflictException("This order has already been paid");
            }

            return new PaymentResponse(
                    existingPayment.getRazorpayOrderId(),
                    order.getId(),
                    existingPayment.getAmount(),
                    existingPayment.getStatus());
        }

        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

        JSONObject options = new JSONObject();
        options.put("amount", Math.round(order.getTotalAmount() * 100));   // paise
        options.put("currency", "INR");
        options.put("receipt", "order_" + order.getId());

        Order razorpayOrder = razorpay.orders.create(options);

        Payment payment = new Payment();
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setOrder(order);

        paymentRepository.save(payment);

        return new PaymentResponse(
                razorpayOrder.get("id"),
                order.getId(),
                order.getTotalAmount(),
                PaymentStatus.CREATED);
    }

    // =====================================================================
    // 2. Browser says "paid" -> verify signature
    // =====================================================================

    // noRollbackFor: FAILED / REFUND_REQUIRED must be saved even though we
    // then throw an error to tell the client.
    @Transactional(noRollbackFor = ApiException.class)
    public PaymentResponse verifyPayment(
            VerifyPaymentRequest request,
            User user) {

        if (request.getRazorpayOrderId() == null
                || request.getRazorpayPaymentId() == null
                || request.getRazorpaySignature() == null) {
            throw new BadRequestException(
                    "razorpayOrderId, razorpayPaymentId and razorpaySignature are required");
        }

        // row lock: a webhook for the same payment waits for us (and vice versa)
        Payment payment = paymentRepository
                .lockByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        com.shopsphere.ecommerce.entity.Order order = payment.getOrder();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }

        // Already verified earlier (or by the webhook): safe to call twice
        if (PaymentStatus.PAID.equals(payment.getStatus())) {
            return response(payment, order);
        }

        // Razorpay signs "order_id|payment_id" with your key secret
        String payload = request.getRazorpayOrderId()
                + "|" + request.getRazorpayPaymentId();

        String expected = hmacSha256Hex(
                payload.getBytes(StandardCharsets.UTF_8), keySecret);

        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8));

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid payment signature");
            paymentRepository.save(payment);
            eventService.record(order, OrderEvent.PAYMENT_FAILED,
                    "Payment could not be verified.");
            throw new BadRequestException("Invalid payment signature");
        }

        boolean confirmed = confirmPayment(
                payment, request.getRazorpayPaymentId(),
                fetchMethodQuietly(request.getRazorpayPaymentId()));

        if (!confirmed) {
            throw new ConflictException(
                    "This order was cancelled before the payment completed. "
                            + "Your payment will be refunded.");
        }

        return response(payment, order);
    }

    // =====================================================================
    // 3. Razorpay webhook (works even if the customer closed the browser)
    // =====================================================================

    @Transactional
    public void handleWebhook(byte[] body, String signature) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BadRequestException("Webhook is not configured");
        }

        if (signature == null) {
            throw new BadRequestException("Missing signature");
        }

        String expected = hmacSha256Hex(body, webhookSecret);

        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("Invalid webhook signature");
        }

        JSONObject json;
        try {
            json = new JSONObject(new String(body, StandardCharsets.UTF_8));
        } catch (JSONException e) {
            throw new BadRequestException("Invalid webhook body");
        }

        String event = json.optString("event", "");
        JSONObject payload = json.optJSONObject("payload");

        log.info("Razorpay webhook received: {}", event);

        switch (event) {
            case "payment.captured", "order.paid" ->
                    onPaymentPaid(entity(payload, "payment"));
            case "payment.failed" ->
                    onPaymentFailed(entity(payload, "payment"));
            case "refund.processed" ->
                    onRefundResult(entity(payload, "refund"), true);
            case "refund.failed" ->
                    onRefundResult(entity(payload, "refund"), false);
            default -> {
                // other events are acknowledged and ignored
            }
        }
    }

    private void onPaymentPaid(JSONObject paymentEntity) {

        if (paymentEntity == null) return;

        String razorpayOrderId = paymentEntity.optString("order_id", null);
        String razorpayPaymentId = paymentEntity.optString("id", null);

        if (razorpayOrderId == null || razorpayPaymentId == null) return;

        paymentRepository.lockByRazorpayOrderId(razorpayOrderId).ifPresent(payment ->
                confirmPayment(payment, razorpayPaymentId,
                        paymentEntity.optString("method", null)));
    }

    private void onPaymentFailed(JSONObject paymentEntity) {

        if (paymentEntity == null) return;

        String razorpayOrderId = paymentEntity.optString("order_id", null);
        if (razorpayOrderId == null) return;

        paymentRepository.lockByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {

            // never downgrade a payment that already succeeded
            if (!PaymentStatus.CREATED.equals(payment.getStatus())
                    && !PaymentStatus.FAILED.equals(payment.getStatus())) {
                return;
            }

            String reason = paymentEntity.optString("error_description", "Payment failed");

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);

            eventService.record(payment.getOrder(), OrderEvent.PAYMENT_FAILED,
                    "Payment attempt failed: " + reason + ". You can try again.");
        });
    }

    private void onRefundResult(JSONObject refundEntity, boolean success) {

        if (refundEntity == null) return;

        String razorpayPaymentId = refundEntity.optString("payment_id", null);
        if (razorpayPaymentId == null) return;

        Payment found = paymentRepository
                .findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
        if (found == null) return;

        Payment payment = paymentRepository
                .lockByOrderId(found.getOrder().getId()).orElse(found);

        if (PaymentStatus.REFUNDED.equals(payment.getStatus())) return;   // already done

        if (success) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRazorpayRefundId(refundEntity.optString("id", payment.getRazorpayRefundId()));
            payment.setRefundedAt(LocalDateTime.now());
            eventService.record(payment.getOrder(), OrderEvent.REFUND_COMPLETED,
                    "Refund of " + Money.inr(payment.getAmount())
                            + " completed to your original payment method.");
        } else {
            payment.setStatus(PaymentStatus.REFUND_REQUIRED);   // scheduler retries
            eventService.record(payment.getOrder(), OrderEvent.REFUND_FAILED,
                    "The refund could not be completed. We are retrying.");
        }

        paymentRepository.save(payment);
    }

    // =====================================================================
    // Shared logic
    // =====================================================================

    /**
     * Marks a payment as received. Used by BOTH the verify call and the
     * webhook, so the result is the same whichever arrives first.
     *
     * @return true  if the order is now CONFIRMED (or already was)
     *         false if the order had been cancelled -> refund flagged instead
     */
    private boolean confirmPayment(Payment payment, String razorpayPaymentId, String method) {

        com.shopsphere.ecommerce.entity.Order order = payment.getOrder();
        String status = payment.getStatus();

        // idempotent: already handled
        if (PaymentStatus.PAID.equals(status)
                || PaymentStatus.REFUNDED.equals(status)
                || PaymentStatus.REFUND_INITIATED.equals(status)) {
            return true;
        }
        if (PaymentStatus.REFUND_REQUIRED.equals(status)) {
            return false;
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        if (method != null && !method.isBlank()) {
            payment.setMethod(method);
        }
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);

        // The order was cancelled (user / 30-minute timeout) before the money
        // arrived. Don't resurrect it - the stock is gone. Refund instead.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            payment.setStatus(PaymentStatus.REFUND_REQUIRED);
            paymentRepository.save(payment);
            eventService.record(order, OrderEvent.REFUND_INITIATED,
                    "Payment of " + Money.inr(payment.getAmount())
                            + " arrived after the order was cancelled. It will be refunded.");
            return false;
        }

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        eventService.record(order, OrderEvent.PAYMENT_CONFIRMED,
                "Payment of " + Money.inr(payment.getAmount())
                        + " received. Your order is confirmed.");
        return true;
    }

    private PaymentResponse response(Payment payment,
                                     com.shopsphere.ecommerce.entity.Order order) {
        return new PaymentResponse(
                payment.getRazorpayOrderId(), order.getId(),
                payment.getAmount(), payment.getStatus());
    }

    /** Payment method (upi/card/...) is nice to show but must never break payment. */
    private String fetchMethodQuietly(String razorpayPaymentId) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            com.razorpay.Payment rp = client.payments.fetch(razorpayPaymentId);
            return rp.has("method") ? rp.<String>get("method") : null;
        } catch (Exception e) {
            log.debug("Could not fetch payment method: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject entity(JSONObject payload, String key) {
        JSONObject wrapper = payload == null ? null : payload.optJSONObject(key);
        return wrapper == null ? null : wrapper.optJSONObject("entity");
    }

    private String hmacSha256Hex(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] hash = mac.doFinal(data);

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Could not verify signature");
        }
    }
}
