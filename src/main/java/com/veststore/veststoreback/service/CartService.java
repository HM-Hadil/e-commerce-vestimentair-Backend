package com.veststore.veststoreback.service;

import com.veststore.veststoreback.dto.CartDto;
import com.veststore.veststoreback.dto.CartItemDto;
import com.veststore.veststoreback.exception.InsufficientStockException;
import com.veststore.veststoreback.exception.ResourceNotFoundException;
import com.veststore.veststoreback.model.*;
;
import com.veststore.veststoreback.repository.CartItemRepository;
import com.veststore.veststoreback.repository.CartRepository;
import com.veststore.veststoreback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Cart getCartByUserId(Long userId) {
        User user = userService.getUserById(userId);
        return cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
    }

    @Transactional
    public Cart addToCart(Long userId, CartItemDto cartItemDto) {
        User user = userService.getUserById(userId);
        Product product = productService.getProductById(cartItemDto.getProductId());

        // Verify that there is enough stock
        if (!product.hasEnoughStock(cartItemDto.getQuantity())) {
            throw new InsufficientStockException("Not enough stock for product: " + product.getName());
        }

        // Get or create user's cart
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        // Check if the product is already in the cart with same size and color
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProductAndSizeAndColor(
                cart, product, String.valueOf(cartItemDto.getSize()), cartItemDto.getColor());

        if (existingItemOpt.isPresent()) {
            // Update existing cart item
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + cartItemDto.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            // Create new cart item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(cartItemDto.getQuantity());
            newItem.setSize(String.valueOf(cartItemDto.getSize()));
            newItem.setColor(cartItemDto.getColor());
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateCartItem(Long userId, Long cartItemId, int quantity) {
        Cart cart = getCartByUserId(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Product product = cartItem.getProduct();

        // Check if update is a quantity increase and verify stock
        int quantityDiff = quantity - cartItem.getQuantity();
        if (quantityDiff > 0 && !product.hasEnoughStock(quantityDiff)) {
            throw new InsufficientStockException("Not enough stock for product: " + product.getName());
        }

        // Update quantity or remove if quantity is 0
        if (quantity > 0) {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        } else {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        Cart cart = getCartByUserId(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear();
        cartItemRepository.deleteByCart(cart);
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public CartDto convertToDto(Cart cart) {
        CartDto cartDto = new CartDto();
        cartDto.setId(cart.getId());
        cartDto.setUserId(cart.getUser().getId());
        cartDto.setTotalAmount(cart.getTotalAmount());

        cartDto.setItems(cart.getItems().stream()
                .map(item -> {
                    CartItemDto itemDto = new CartItemDto();
                    itemDto.setId(item.getId());
                    itemDto.setProductId(item.getProduct().getId());
                    itemDto.setProductName(item.getProduct().getName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setSize(ProductSize.valueOf(item.getSize()));
                    itemDto.setColor(item.getColor());
                    itemDto.setPrice(item.getProduct().getPrice());
                    return itemDto;
                })
                .toList());

        return cartDto;
    }
}