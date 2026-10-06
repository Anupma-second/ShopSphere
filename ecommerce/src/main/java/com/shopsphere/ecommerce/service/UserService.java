package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.UserResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.UserNotFoundException;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // ---------- My account ----------

    public UserResponse me(User current) {
        return toResponse(reload(current));
    }

    @Transactional
    public UserResponse updateName(User current, String name) {
        User user = reload(current);
        user.setName(name.trim());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User current, String currentPassword, String newPassword) {
        User user = reload(current);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Your current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // the principal comes from the JWT filter; always work on a fresh copy
    private User reload(User current) {
        return userRepository.findById(current.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}