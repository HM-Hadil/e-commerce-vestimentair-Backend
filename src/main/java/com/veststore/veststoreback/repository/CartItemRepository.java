package com.veststore.veststoreback.repository;


import com.veststore.veststoreback.model.Cart;
import com.veststore.veststoreback.model.CartItem;
import com.veststore.veststoreback.model.CartStatus;
import com.veststore.veststoreback.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByCart(Cart cart);
    Optional<CartItem> findByCartAndProductAndSizeAndColor(Cart cart, Product product, String size, String color);

    List<CartItem> findByStatus(CartStatus status);
}