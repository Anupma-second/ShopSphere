package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.AddToCartRequest;
import com.shopsphere.ecommerce.dto.CartItemResponse;
import com.shopsphere.ecommerce.dto.UpdateCartQuantityRequest;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.CartItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FIX: the old POST / , GET /{id} and PUT /{id} endpoints accepted a raw
 * CartItem (with a user id from the request body) and returned entities.
 * Only the safe, user-scoped endpoints remain.
 */
@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {

    private final CartItemService cartItemService;

    public CartItemController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @PostMapping("/add")
    public CartItemResponse addToCart(
            @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal User user) {
        return cartItemService.addToCart(request, user);
    }

    @GetMapping
    public List<CartItemResponse> getMyCart(@AuthenticationPrincipal User user) {
        return cartItemService.getCartByUserId(user.getId());
    }

    @PutMapping("/{id}/quantity")
    public CartItemResponse updateQuantity(
            @PathVariable Long id,
            @RequestBody UpdateCartQuantityRequest request,
            @AuthenticationPrincipal User user) {
        return cartItemService.updateQuantity(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        cartItemService.deleteCartItem(id, user);
        return ResponseEntity.noContent().build();
    }
}
