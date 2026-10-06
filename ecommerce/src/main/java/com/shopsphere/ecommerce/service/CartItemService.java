package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.CartItemResponse;
import com.shopsphere.ecommerce.entity.CartItem;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.CartItemRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.shopsphere.ecommerce.dto.AddToCartRequest;
import com.shopsphere.ecommerce.dto.UpdateCartQuantityRequest;

import java.util.List;
import java.util.Optional;

@Service
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartItemService(
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {

        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public CartItem createCartItem(CartItem cartItem) {

        Long userId = cartItem.getUser().getId();
        Long productId = cartItem.getProduct().getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        cartItem.setUser(user);
        cartItem.setProduct(product);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getAllCartItems() {
        return cartItemRepository.findAll();
    }

    public Optional<CartItem> getCartItemById(Long id) {
        return cartItemRepository.findById(id);
    }

    public CartItem updateCartItem(Long id, CartItem updatedCartItem) {

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cart item not found with id: " + id));

        cartItem.setQuantity(updatedCartItem.getQuantity());

        return cartItemRepository.save(cartItem);
    }

    public void deleteCartItem(Long id, User user) {

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Cart item not found"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot delete another user's cart item");
        }

        cartItemRepository.delete(cartItem);
    }

    public CartItemResponse addToCart(
            AddToCartRequest request,
            User user) {

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        if (request.getQuantity() <= 0) {
            throw new RuntimeException(
                    "Quantity must be greater than 0");
        }

        if (request.getQuantity() > product.getStock()) {
            throw new RuntimeException(
                    "Not enough stock available");
        }

        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(
                        user.getId(),
                        request.getProductId()
                )
                .orElse(null);

        if (cartItem != null) {

            int newQuantity =
                    cartItem.getQuantity() + request.getQuantity();

            if (newQuantity > product.getStock()) {
                throw new RuntimeException(
                        "Not enough stock available");
            }

            cartItem.setQuantity(newQuantity);

        } else {

            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
        }

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        double totalPrice =
                product.getPrice() * savedCartItem.getQuantity();

        return new CartItemResponse(
                savedCartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                savedCartItem.getQuantity(),
                totalPrice
        ).withProduct(product);
    }

    public List<CartItemResponse> getCartByUserId(Long userId) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        return cartItems.stream()
                .map(cartItem -> {

                    Product product = cartItem.getProduct();

                    double totalPrice =
                            product.getPrice() * cartItem.getQuantity();

                    return new CartItemResponse(
                            cartItem.getId(),
                            product.getId(),
                            product.getName(),
                            product.getPrice(),
                            cartItem.getQuantity(),
                            totalPrice
                    ).withProduct(product);
                })
                .toList();
    }

    public CartItemResponse updateQuantity(
            Long cartItemId,
            UpdateCartQuantityRequest request,
            User user) {

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new RuntimeException("Cart item not found"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You cannot modify another user's cart");
        }

        if (request.getQuantity() <= 0) {
            throw new RuntimeException(
                    "Quantity must be greater than 0");
        }

        Product product = cartItem.getProduct();

        if (request.getQuantity() > product.getStock()) {
            throw new RuntimeException(
                    "Not enough stock available");
        }

        cartItem.setQuantity(request.getQuantity());

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        double totalPrice =
                product.getPrice() * savedCartItem.getQuantity();

        return new CartItemResponse(
                savedCartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                savedCartItem.getQuantity(),
                totalPrice
        ).withProduct(product);
    }
}