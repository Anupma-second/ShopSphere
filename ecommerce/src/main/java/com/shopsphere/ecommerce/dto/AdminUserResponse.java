package com.shopsphere.ecommerce.dto;

import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        boolean emailVerified) {

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.isEnabled(), u.isEmailVerified());
    }
}
