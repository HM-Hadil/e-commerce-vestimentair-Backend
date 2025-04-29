package com.veststore.veststoreback.repository;

import com.veststore.veststoreback.model.Order;
import com.veststore.veststoreback.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
}
