package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(
            Long userId,
            Long productId
    );

    List<CartItem> findByUserId(Long userId);
}