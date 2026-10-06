package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Called by Razorpay's servers (not by the browser), so it is public in
 * SecurityConfig - the HMAC signature in the header is what authenticates it.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // raw bytes: the signature is computed over the exact body Razorpay sent
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false)
            String signature) {

        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
