package com.veststore.veststoreback.service;

import com.veststore.veststoreback.dto.CartDto;
import com.veststore.veststoreback.dto.CartItemDto;
import com.veststore.veststoreback.exception.InsufficientStockException;
import com.veststore.veststoreback.exception.ResourceNotFoundException;
import com.veststore.veststoreback.model.*;
import com.veststore.veststoreback.repository.CartItemRepository;
import com.veststore.veststoreback.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            newItem.setStatus(CartStatus.EN_ATTENTE);
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

    public List<CartItemDto> getAllCartItems() {
        List<CartItem> items = cartItemRepository.findAll();

        return items.stream().map(item -> {
            CartItemDto dto = new CartItemDto();
            dto.setId(item.getId());
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setQuantity(item.getQuantity());
            dto.setSize(item.getProduct().getSize());
            dto.setColor(item.getColor());
            dto.setStatus(item.getStatus());
            dto.setPrice(item.getProduct().getPrice());
            dto.setStock(item.getProduct().getStock());

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void removeCartItem(Long cartItemId, Long userId) {
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
                    itemDto.setStatus(item.getStatus()); // Ajout du statut
                    return itemDto;
                })
                .toList());

        return cartDto;
    }

    // ---- Nouvelles fonctionnalités pour gérer les statuts des commandes ----

    /**
     * Place une commande (passe tous les articles du panier au statut VALIDEE)
     */
    @Transactional
    public Cart placeOrder(Long userId) {
        Cart cart = getCartByUserId(userId);

        // Vérifier le stock pour tous les articles du panier
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.hasEnoughStock(item.getQuantity())) {
                throw new InsufficientStockException("Not enough stock for product: " + product.getName());
            }

            // Mise à jour du stock
            int newStock = product.getStock() - item.getQuantity();
            product.setStock(newStock);
            productService.updateProduct(product);
        }

        // Mettre à jour le statut des articles
        for (CartItem item : cart.getItems()) {
            item.setStatus(CartStatus.VALIDEE);
            cartItemRepository.save(item);
        }

        return cartRepository.save(cart);
    }

    /**
     * Obtient toutes les commandes d'un utilisateur filtrées par statut
     */
    @Transactional(readOnly = true)
    public List<CartItemDto> getOrdersByStatus(Long userId, CartStatus status) {
        Cart cart = getCartByUserId(userId);

        return cart.getItems().stream()
                .filter(item -> item.getStatus() == status)
                .map(item -> {
                    CartItemDto dto = new CartItemDto();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProduct().getId());
                    dto.setProductName(item.getProduct().getName());
                    dto.setQuantity(item.getQuantity());
                    dto.setSize(ProductSize.valueOf(item.getSize()));
                    dto.setColor(item.getColor());
                    dto.setStatus(item.getStatus());
                    dto.setPrice(item.getProduct().getPrice());
                    dto.setStock(item.getProduct().getStock());
                    dto.setStock(item.getProduct().getStock());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Annule une commande (possible seulement si EN_ATTENTE)
     */
    @Transactional
    public void cancelOrder(Long userId, Long cartItemId) {
        Cart cart = getCartByUserId(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (cartItem.getStatus() != CartStatus.EN_ATTENTE) {
            throw new IllegalStateException("Cannot cancel order that is not in waiting status");
        }

        cartItem.setStatus(CartStatus.ANNULEE);
        cartItemRepository.save(cartItem);
    }

    /**
     * Changement de statut d'un article (pour les admins)
     */
    @Transactional
    public void updateOrderStatus(Long cartItemId, CartStatus newStatus) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        // Validation des transitions d'état
        switch (newStatus) {
            case VALIDEE:
                if (cartItem.getStatus() != CartStatus.EN_ATTENTE) {
                    throw new IllegalStateException("Order must be in EN_ATTENTE status to be validated");
                }
                break;
            case EXPEDIEE:
                if (cartItem.getStatus() != CartStatus.VALIDEE) {
                    throw new IllegalStateException("Order must be in VALIDEE status to be shipped");
                }
                break;
            case LIVREE:
                if (cartItem.getStatus() != CartStatus.EXPEDIEE) {
                    throw new IllegalStateException("Order must be in EXPEDIEE status to be delivered");
                }
                break;
            case ANNULEE:
                if (cartItem.getStatus() == CartStatus.EXPEDIEE || cartItem.getStatus() == CartStatus.LIVREE) {
                    throw new IllegalStateException("Cannot cancel an order that is already shipped or delivered");
                }
                // Si annulation d'une commande déjà validée, remettre le stock
                if (cartItem.getStatus() == CartStatus.VALIDEE) {
                    Product product = cartItem.getProduct();
                    product.setStock(product.getStock() + cartItem.getQuantity());
                    productService.updateProduct(product);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        cartItem.setStatus(newStatus);
        cartItemRepository.save(cartItem);
    }

    /**
     * Obtenir toutes les commandes par statut (pour les admins)
     */
    @Transactional(readOnly = true)
    public List<CartItemDto> getAllOrdersByStatus(CartStatus status) {
        List<CartItem> items = cartItemRepository.findByStatus(status);

        return items.stream().map(item -> {
            CartItemDto dto = new CartItemDto();
            dto.setId(item.getId());
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setQuantity(item.getQuantity());
            dto.setSize(ProductSize.valueOf(item.getSize()));
            dto.setColor(item.getColor());
            dto.setStatus(item.getStatus());
            dto.setPrice(item.getProduct().getPrice());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Vérifier les produits à stock faible
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsWithLowStock() {
        return productService.getAllProducts().stream()
                .filter(Product::hasLowStock)
                .collect(Collectors.toList());
    }

    // NEW METHOD: Allow users to update their own order status (limited to VALIDEE/ANNULEE)
    @Transactional
    public void updateOrderStatusByUser(Long userId, Long cartItemId, CartStatus newStatus) {
        // Only VALIDEE and ANNULEE statuses are allowed for user updates
        if (newStatus != CartStatus.VALIDEE && newStatus != CartStatus.ANNULEE) {
            throw new IllegalArgumentException("Users can only set order status to VALIDEE or ANNULEE");
        }

        // Get the cart item
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        // Verify that the cart item belongs to the user
        Cart cart = getCartByUserId(userId);
        boolean itemBelongsToUser = cart.getItems().stream()
                .anyMatch(item -> item.getId().equals(cartItemId));

        if (!itemBelongsToUser) {
            throw new AccessDeniedException("You don't have permission to update this order");
        }

        // Validation for VALIDEE status
        if (newStatus == CartStatus.VALIDEE) {
            if (cartItem.getStatus() != CartStatus.EN_ATTENTE) {
                throw new IllegalStateException("Order must be in EN_ATTENTE status to be validated");
            }

            // Verify sufficient stock
            Product product = cartItem.getProduct();
            if (!product.hasEnoughStock(cartItem.getQuantity())) {
                throw new InsufficientStockException("Not enough stock for product: " + product.getName());
            }

            // Update product stock
            int newStock = product.getStock() - cartItem.getQuantity();
            product.setStock(newStock);
            productService.updateProduct(product);
        }

        // Validation for ANNULEE status
        if (newStatus == CartStatus.ANNULEE) {
            if (cartItem.getStatus() == CartStatus.EXPEDIEE || cartItem.getStatus() == CartStatus.LIVREE) {
                throw new IllegalStateException("Cannot cancel an order that is already shipped or delivered");
            }

            // Return stock if cancelling a validated order
            if (cartItem.getStatus() == CartStatus.VALIDEE) {
                Product product = cartItem.getProduct();
                product.setStock(product.getStock() + cartItem.getQuantity());
                productService.updateProduct(product);
            }
        }

        // Update the status
        cartItem.setStatus(newStatus);
        cartItemRepository.save(cartItem);
    }
}