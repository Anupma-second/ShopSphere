package com.shopsphere.ecommerce.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.shopsphere.ecommerce.dto.CreatePaymentRequest;
import com.shopsphere.ecommerce.dto.PaymentResponse;
import com.shopsphere.ecommerce.dto.VerifyPaymentRequest;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Payment;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.ApiException;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ConflictException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public PaymentResponse createPayment(
            CreatePaymentRequest request,
            User user) throws Exception {

        // FIX: ownership checked in the query; foreign orders look "not found"
        com.shopsphere.ecommerce.entity.Order order =
                orderRepository.findByIdAndUserId(request.getOrderId(), user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Order not found"));

        // FIX: only an order that is still waiting for payment can be paid
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "This order is " + order.getStatus()
                            + " and cannot be paid");
        }

        Payment existingPayment = paymentRepository
                .findByOrderId(order.getId())
                .orElse(null);

        if (existingPayment != null) {

            if ("PAID".equals(existingPayment.getStatus())) {
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

        // Razorpay expects the amount in paise
        options.put("amount", Math.round(order.getTotalAmount() * 100));
        options.put("currency", "INR");
        options.put("receipt", "order_" + order.getId());

        Order razorpayOrder = razorpay.orders.create(options);

        Payment payment = new Payment();
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus("CREATED");
        payment.setOrder(order);

        paymentRepository.save(payment);

        return new PaymentResponse(
                razorpayOrder.get("id"),
                order.getId(),
                order.getTotalAmount(),
                "CREATED");
    }

    // noRollbackFor: the FAILED / REFUND_REQUIRED status must be saved even
    // though we then throw an error to tell the client.
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

        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        com.shopsphere.ecommerce.entity.Order order = payment.getOrder();

        // same message as "not found" so payment ids cannot be probed
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }

        // Already verified earlier: safe to call twice
        if ("PAID".equals(payment.getStatus())) {
            return new PaymentResponse(
                    payment.getRazorpayOrderId(), order.getId(),
                    payment.getAmount(), "PAID");
        }

        // Razorpay signs "order_id|payment_id" with your key secret
        String payload = request.getRazorpayOrderId()
                + "|" + request.getRazorpayPaymentId();

        String expectedSignature = hmacSha256(payload, keySecret);

        boolean valid = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8));

        if (!valid) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new BadRequestException("Invalid payment signature");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());

        // FIX: the order was cancelled (timeout / user) before the money
        // arrived. Do NOT resurrect it (stock was already released); record
        // that the customer must be refunded.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            payment.setStatus("REFUND_REQUIRED");
            paymentRepository.save(payment);
            throw new ConflictException(
                    "This order was cancelled before the payment completed. "
                            + "Your payment will be refunded.");
        }

        payment.setStatus("PAID");
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return new PaymentResponse(
                payment.getRazorpayOrderId(), order.getId(),
                payment.getAmount(), "PAID");
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Could not verify payment signature");
        }
    }
}
