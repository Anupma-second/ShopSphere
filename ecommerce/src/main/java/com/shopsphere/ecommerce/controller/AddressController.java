package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.AddressRequest;
import com.shopsphere.ecommerce.dto.SavedAddressResponse;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<SavedAddressResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.create(user, request));
    }

    @GetMapping
    public List<SavedAddressResponse> mine(@AuthenticationPrincipal User user) {
        return addressService.list(user);
    }

    @GetMapping("/{id}")
    public SavedAddressResponse getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return addressService.get(user, id);
    }

    @PutMapping("/{id}")
    public SavedAddressResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return addressService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        addressService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
