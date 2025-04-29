package com.veststore.veststoreback.repository;


import com.veststore.veststoreback.model.Cart;
import com.veststore.veststoreback.model.CartItem;
import com.veststore.veststoreback.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProductAndSizeAndColor(Cart cart, Product product, String size, String color);
    void deleteByCart(Cart cart);
}