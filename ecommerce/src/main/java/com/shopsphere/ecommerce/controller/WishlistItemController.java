package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.WishlistItemResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.entity.WishlistItem;
import com.shopsphere.ecommerce.service.WishlistItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist-items")
public class WishlistItemController {

    private final WishlistItemService wishlistItemService;

    public WishlistItemController(WishlistItemService wishlistItemService) {
        this.wishlistItemService = wishlistItemService;
    }

    public record WishlistRequest(Long productId) {}

    private static WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice());
    }

    @PostMapping
    public ResponseEntity<WishlistItemResponse> add(
            @AuthenticationPrincipal User user,
            @RequestBody WishlistRequest request) {
        WishlistItem saved =
                wishlistItemService.addForUser(user, request.productId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    public List<WishlistItemResponse> getMine(
            @AuthenticationPrincipal User user) {
        return wishlistItemService.getForUser(user).stream()
                .map(WishlistItemController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WishlistItemResponse> getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return wishlistItemService.getForUserById(user, id)
                .map(item -> ResponseEntity.ok(toResponse(item)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        boolean deleted = wishlistItemService.deleteForUser(user, id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
