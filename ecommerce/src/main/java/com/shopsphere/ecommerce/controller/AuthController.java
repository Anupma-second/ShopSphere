package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.AuthResponse;
import com.shopsphere.ecommerce.dto.ForgotPasswordRequest;
import com.shopsphere.ecommerce.dto.LoginRequest;
import com.shopsphere.ecommerce.dto.RegisterRequest;
import com.shopsphere.ecommerce.dto.ResetPasswordRequest;
import com.shopsphere.ecommerce.dto.UserResponse;
import com.shopsphere.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // body is the raw refresh token string (quotes tolerated)
    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody String refreshToken) {
        return authService.refreshAccessToken(
                refreshToken.replace("\"", "").trim());
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.getEmail());
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(
                request.getToken(), request.getNewPassword());
    }
}
