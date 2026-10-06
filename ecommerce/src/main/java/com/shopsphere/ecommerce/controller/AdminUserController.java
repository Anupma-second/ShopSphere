package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.AdminUserResponse;
import com.shopsphere.ecommerce.dto.PageResponse;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.AdminUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Manage users and sellers. "Manage sellers" = GET /api/admin/users?role=SELLER;
 * make someone a seller with PUT /{id}/role?role=SELLER.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return adminUserService.list(q, role, pageable);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable Long id) {
        return adminUserService.get(id);
    }

    @PutMapping("/{id}/role")
    public AdminUserResponse changeRole(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestParam Role role) {
        return adminUserService.changeRole(id, role, admin);
    }

    /** enabled=false blocks the account (all its tokens stop working). */
    @PutMapping("/{id}/enabled")
    public AdminUserResponse setEnabled(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return adminUserService.setEnabled(id, enabled, admin);
    }
}