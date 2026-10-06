package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.ChangePasswordRequest;
import com.shopsphere.ecommerce.dto.UpdateProfileRequest;
import com.shopsphere.ecommerce.dto.UserResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** The logged-in user's own account (any role). */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal User user) {
        return userService.me(user);
    }

    @PutMapping
    public UserResponse updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateName(user, request.name());
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
