package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository
        extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserId(Long userId);

    Optional<WishlistItem> findByUserIdAndProductId(
            Long userId,
            Long productId
    );
}