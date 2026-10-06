package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.AuthResponse;
import com.shopsphere.ecommerce.dto.ForgotPasswordRequest;
import com.shopsphere.ecommerce.dto.LoginRequest;
import com.shopsphere.ecommerce.dto.RegisterRequest;
import com.shopsphere.ecommerce.dto.ResetPasswordRequest;
import com.shopsphere.ecommerce.dto.UserResponse;
import com.shopsphere.ecommerce.exception.UnauthorizedException;
import com.shopsphere.ecommerce.security.LoginRateLimiter;
import com.shopsphere.ecommerce.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService,
                          LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();

        // 429 before even checking the password if there were too many failures
        loginRateLimiter.check(request.getEmail(), ip);

        try {
            AuthResponse response = authService.login(request);
            loginRateLimiter.recordSuccess(request.getEmail(), ip);
            return response;
        } catch (UnauthorizedException wrongPassword) {
            loginRateLimiter.recordFailure(request.getEmail(), ip);
            throw wrongPassword;
        }
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