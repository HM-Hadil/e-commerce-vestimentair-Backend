package com.veststore.veststoreback.repository;

import com.veststore.veststoreback.model.Order;
import com.veststore.veststoreback.model.OrderStatus;
import com.veststore.veststoreback.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserAndStatus(User user, OrderStatus status);
}
