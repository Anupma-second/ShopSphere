package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.AdminUserResponse;
import com.shopsphere.ecommerce.dto.PageResponse;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** q searches name/email; role filters (e.g. SELLER for "manage sellers"). */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(String q, Role role, Pageable pageable) {

        String query = (q == null || q.isBlank()) ? null : q.trim();
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);

        return PageResponse.of(
                userRepository.search(query, role,
                        PageRequest.of(pageable.getPageNumber(), size, pageable.getSort())),
                AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(Long id) {
        return AdminUserResponse.from(find(id));
    }

    /**
     * Takes effect on the user's next request (the JWT filter reloads the
     * role from the database every time).
     */
    @Transactional
    public AdminUserResponse changeRole(Long id, Role role, User admin) {

        if (role == null) {
            throw new BadRequestException("role is required");
        }

        if (admin.getId().equals(id)) {
            throw new BadRequestException("You cannot change your own role");
        }

        User user = find(id);
        user.setRole(role);

        return AdminUserResponse.from(userRepository.save(user));
    }

    /** Disabled users are rejected by the JWT filter immediately. */
    @Transactional
    public AdminUserResponse setEnabled(Long id, boolean enabled, User admin) {

        if (admin.getId().equals(id) && !enabled) {
            throw new BadRequestException("You cannot disable your own account");
        }

        User user = find(id);
        user.setEnabled(enabled);

        return AdminUserResponse.from(userRepository.save(user));
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
