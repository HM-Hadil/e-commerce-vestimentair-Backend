package com.veststore.veststoreback.repository;

import com.veststore.veststoreback.model.Cart;
import com.veststore.veststoreback.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}