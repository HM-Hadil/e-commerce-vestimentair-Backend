package com.veststore.veststoreback.controller;


import com.veststore.veststoreback.dto.OrderDto;
import com.veststore.veststoreback.model.Order;
import com.veststore.veststoreback.model.OrderStatus;
import com.veststore.veststoreback.security.UserDetailsImpl;
import com.veststore.veststoreback.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderService.getOrderById(id);

        // If user is not admin, check if the order belongs to the user
        if (!userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Long userId = extractUserId(userDetails);
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<Order>> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/user/status/{status}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<Order>> getOrdersByUserAndStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable OrderStatus status) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(orderService.getOrdersByUserAndStatus(userId, status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Order> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderDto orderDto) {
        Long userId = extractUserId(userDetails);
        return new ResponseEntity<>(orderService.createOrderFromCart(userId, String.valueOf(orderDto)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Order> validateOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.validateOrder(id));
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Order> shipOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.shipOrder(id));
    }

    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Order> deliverOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.deliverOrder(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderService.getOrderById(id);

        // If user is not admin, check if the order belongs to the user
        if (!userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Long userId = extractUserId(userDetails);
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    // Helper method to extract user ID from UserDetails
    private Long extractUserId(UserDetails userDetails) {
        // This implementation depends on your UserDetails implementation
        return ((UserDetailsImpl) userDetails).getUserId();
    }
}
