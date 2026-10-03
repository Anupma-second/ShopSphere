package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.entity.WishlistItem;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WishlistItemService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    public WishlistItemService(
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository) {

        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
    }

    public WishlistItem addForUser(User user, Long productId) {

        if (productId == null) {
            throw new RuntimeException("productId is required");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        // Already in the wishlist? Return it instead of adding a duplicate.
        if (wishlistItemRepository
                .findByUserIdAndProductId(user.getId(), productId)
                .isPresent()) {

            throw new com.shopsphere.ecommerce.exception.ConflictException(
                    "Product already in wishlist");
        }

        return wishlistItemRepository.save(
                new WishlistItem(user, product));
    }

    public List<WishlistItem> getForUser(User user) {
        return wishlistItemRepository.findByUserId(user.getId());
    }

    public Optional<WishlistItem> getForUserById(User user, Long id) {
        return wishlistItemRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()));
    }

    public boolean deleteForUser(User user, Long id) {
        Optional<WishlistItem> item = getForUserById(user, id);

        if (item.isEmpty()) {
            return false;
        }

        wishlistItemRepository.delete(item.get());
        return true;
    }
}