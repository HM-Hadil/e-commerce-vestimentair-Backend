package com.veststore.veststoreback.controller;

import com.veststore.veststoreback.dto.CartDto;
import com.veststore.veststoreback.dto.CartItemDto;
import com.veststore.veststoreback.model.Cart;
import com.veststore.veststoreback.security.UserDetailsImpl;
import com.veststore.veststoreback.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CartDto> addItemToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemDto cartItemDto) {
        Long userId = extractUserId(userDetails);
        Cart cart = cartService.addToCart(userId, cartItemDto);
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CartDto> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemDto cartItemDto) {
        Long userId = extractUserId(userDetails);
        Cart cart = cartService.updateCartItem(userId, itemId, cartItemDto.getQuantity());
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> removeItemFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        Long userId = extractUserId(userDetails);
        cartService.removeCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    // Helper method to extract user ID from UserDetails
    private Long extractUserId(UserDetails userDetails) {
        // This implementation depends on your UserDetails implementation
        // For example, if you have a custom UserDetails implementation with a getUserId method
        return ((UserDetailsImpl) userDetails).getUserId();
    }
}