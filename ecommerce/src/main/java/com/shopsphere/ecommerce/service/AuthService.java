package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.AuthResponse;
import com.shopsphere.ecommerce.dto.LoginRequest;
import com.shopsphere.ecommerce.dto.RegisterRequest;
import com.shopsphere.ecommerce.dto.UserResponse;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.DuplicateUserException;
import com.shopsphere.ecommerce.exception.ForbiddenException;
import com.shopsphere.ecommerce.exception.InvalidTokenException;
import com.shopsphere.ecommerce.exception.UnauthorizedException;
import com.shopsphere.ecommerce.repository.UserRepository;
import com.shopsphere.ecommerce.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    // false in dev so you are not blocked without SMTP; true in production
    @Value("${app.require-email-verification:false}")
    private boolean requireEmailVerification;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public UserResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException(
                    "User already exists with email: " + email);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);          // role is never taken from input
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());

        User saved = userRepository.save(user);

        emailService.send(
                saved.getEmail(),
                "Verify your ShopSphere email",
                "Hi " + saved.getName() + ",\n\n"
                        + "Please verify your email by opening this link:\n"
                        + backendUrl + "/api/auth/verify?token="
                        + saved.getVerificationToken()
                        + "\n\nIf you did not create this account, ignore this email.");

        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // FIX: disabled accounts cannot log in
        if (!user.isEnabled()) {
            throw new ForbiddenException("This account has been disabled");
        }

        // FIX: optional enforcement of verified email
        if (requireEmailVerification && !user.isEmailVerified()) {
            throw new ForbiddenException(
                    "Please verify your email before logging in");
        }

        return new AuthResponse(
                jwtService.generateToken(user.getEmail()),
                jwtService.generateRefreshToken(user.getEmail()),
                toResponse(user));
    }

    public AuthResponse refreshAccessToken(String refreshToken) {

        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // FIX: only genuine refresh tokens can be exchanged
        if (!jwtService.isRefreshToken(claims)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(claims.getSubject())
                .filter(User::isEnabled)
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid refresh token"));

        return new AuthResponse(
                jwtService.generateToken(user.getEmail()),
                refreshToken,
                toResponse(user));
    }

    public String verifyEmail(String token) {

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() ->
                        new InvalidTokenException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return "Email verified successfully!";
    }

    /**
     * FIX (critical): the token is emailed, never returned in the response,
     * and the response is identical whether or not the email exists, so this
     * endpoint can no longer be used to take over accounts or list users.
     */
    public String forgotPassword(String rawEmail) {

        String email = rawEmail.trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {

            String resetToken = UUID.randomUUID().toString();

            user.setResetToken(resetToken);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            emailService.send(
                    user.getEmail(),
                    "Reset your ShopSphere password",
                    "Hi " + user.getName() + ",\n\n"
                            + "Use this link to reset your password "
                            + "(valid for 15 minutes):\n"
                            + frontendUrl + "/reset-password?token=" + resetToken
                            + "\n\nIf you did not request this, ignore this email.");
        });

        return "If an account exists for that email, a reset link has been sent.";
    }

    public String resetPassword(String token, String newPassword) {

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() ->
                        new InvalidTokenException("Invalid reset token"));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return "Password reset successfully!";
    }
}
