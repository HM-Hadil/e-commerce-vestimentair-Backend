package com.veststore.veststoreback.controller;

import com.veststore.veststoreback.dto.CartDto;
import com.veststore.veststoreback.dto.CartItemDto;
import com.veststore.veststoreback.model.Cart;
import com.veststore.veststoreback.model.CartStatus;
import com.veststore.veststoreback.model.Product;
import com.veststore.veststoreback.security.UserDetailsImpl;
import com.veststore.veststoreback.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartDto> getCart(@PathVariable Long userId) {
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItemToCart(
            @RequestParam Long userId,
            @RequestBody CartItemDto cartItemDto) {
        Cart cart = cartService.addToCart(userId, cartItemDto);
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam Long userId,
            @RequestBody Integer quantity) {
        Cart cart = cartService.updateCartItem(userId, itemId, quantity);
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @GetMapping
    public List<CartItemDto> getAllCartItems() {
        return cartService.getAllCartItems();
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long cartItemId, @RequestParam Long userId) {
        cartService.removeCartItem(cartItemId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    // Nouvelles méthodes pour les fonctionnalités de commande

    @PostMapping("/checkout")
    public ResponseEntity<CartDto> placeOrder(@RequestParam Long userId) {
        Cart cart = cartService.placeOrder(userId);
        return ResponseEntity.ok(cartService.convertToDto(cart));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<CartItemDto>> getOrdersByStatus(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "VALIDEE") CartStatus status) {
        List<CartItemDto> orders = cartService.getOrdersByStatus(userId, status);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/orders/{cartItemId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestParam Long userId,
            @PathVariable Long cartItemId) {
        cartService.cancelOrder(userId, cartItemId);
        return ResponseEntity.ok().build();
    }

    // NEW ENDPOINT: Allow users to update their own order status (limited to VALIDEE/ANNULEE)
    @PutMapping("/items/{cartItemId}/status")
    public ResponseEntity<Void> updateUserOrderStatus(
            @PathVariable Long cartItemId,
            @RequestParam Long userId,
            @RequestParam CartStatus status) {
        // Only allow users to set status to VALIDEE or ANNULEE
        if (status != CartStatus.VALIDEE && status != CartStatus.ANNULEE) {
            return ResponseEntity.badRequest().build();
        }

        // Check if the order belongs to the user (this should be implemented in the service)
        cartService.updateOrderStatusByUser(userId, cartItemId, status);
        return ResponseEntity.ok().build();
    }

    // Endpoints admin

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<CartItemDto>> getAllOrdersByStatus(
            @RequestParam(required = false, defaultValue = "VALIDEE") CartStatus status) {
        List<CartItemDto> orders = cartService.getAllOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/admin/orders/{cartItemId}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long cartItemId,
            @RequestParam CartStatus status) {
        cartService.updateOrderStatus(cartItemId, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/lowstock")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<Product>> getProductsWithLowStock() {
        List<Product> products = cartService.getProductsWithLowStock();
        return ResponseEntity.ok(products);
    }
}