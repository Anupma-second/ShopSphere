package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByResetToken(String resetToken);

    // ---- Admin ----

    /** q matches name or email (case-insensitive); a null filter is ignored. */
    @Query("select u from User u where (:role is null or u.role = :role) "
            + "and (:q is null "
            + "or lower(u.email) like lower(concat('%', :q, '%')) "
            + "or lower(u.name) like lower(concat('%', :q, '%')))")
    Page<User> search(@Param("q") String q, @Param("role") Role role, Pageable pageable);

    long countByRole(Role role);
}