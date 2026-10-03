package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.CreatePaymentRequest;
import com.shopsphere.ecommerce.dto.PaymentResponse;
import com.shopsphere.ecommerce.dto.VerifyPaymentRequest;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public PaymentResponse createPayment(
            @RequestBody CreatePaymentRequest request,
            Authentication authentication)
            throws Exception {

        User user = (User) authentication.getPrincipal();

        return paymentService.createPayment(request, user);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @RequestBody VerifyPaymentRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        paymentService.verifyPayment(request, user);

        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
}
