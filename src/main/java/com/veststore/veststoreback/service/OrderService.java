package com.veststore.veststoreback.service;

import com.veststore.veststoreback.dto.OrderDto;
import com.veststore.veststoreback.dto.OrderItemDto;
import com.veststore.veststoreback.exception.InsufficientStockException;
import com.veststore.veststoreback.exception.OrderStatusException;
import com.veststore.veststoreback.exception.ResourceNotFoundException;
import com.veststore.veststoreback.model.*;
import com.veststore.veststoreback.repository.OrderItemRepository;
import com.veststore.veststoreback.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        User user = userService.getUserById(userId);
        return orderRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserAndStatus(Long userId, OrderStatus status) {
        User user = userService.getUserById(userId);
        return orderRepository.findByUserAndStatus(user, status);
    }

    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress) {
        User user = userService.getUserById(userId);
        Cart cart = cartService.getCartByUserId(userId);

        if (cart.getItems().isEmpty()) {
            throw new ResourceNotFoundException("Cannot create order from empty cart");
        }

        // Check stock for all items
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!product.hasEnoughStock(cartItem.getQuantity())) {
                throw new InsufficientStockException("Not enough stock for product: " + product.getName());
            }
        }

        // Create new order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        // Set estimated delivery date to 5 days from now
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(5));
        order.setStatus(OrderStatus.EN_ATTENTE);
        order.setShippingAddress(shippingAddress);

        // Add items to the order
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setSize(cartItem.getSize());
            orderItem.setColor(cartItem.getColor());

            order.getItems().add(orderItem);
        }

        // Calculate total
        order.calculateTotal();

        // Save the order
        Order savedOrder = orderRepository.save(order);

        // Clear the cart
        cartService.clearCart(userId);

        return savedOrder;
    }

    @Transactional
    public Order validateOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (!order.canValidate()) {
            throw new OrderStatusException("Cannot validate order with status: " + order.getStatus());
        }

        // Verify stock and decrement stock for each item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!product.hasEnoughStock(item.getQuantity())) {
                throw new InsufficientStockException("Not enough stock for product: " + product.getName());
            }

            // Decrement stock
            productService.updateStock(product.getId(), -item.getQuantity());
        }

        // Update order status
        order.setStatus(OrderStatus.VALIDEE);
        return orderRepository.save(order);
    }

    @Transactional
    public Order shipOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (!order.canShip()) {
            throw new OrderStatusException("Cannot ship order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.EXPEDIEE);
        return orderRepository.save(order);
    }

    @Transactional
    public Order deliverOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (!order.canDeliver()) {
            throw new OrderStatusException("Cannot deliver order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.LIVREE);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (!order.canCancel()) {
            throw new OrderStatusException("Cannot cancel order with status: " + order.getStatus());
        }

        // If the order was already validated, we need to restock the items
        if (order.getStatus() == OrderStatus.VALIDEE) {
            for (OrderItem item : order.getItems()) {
                // Restock the products
                productService.updateStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.ANNULEE);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public OrderDto convertToDto(Order order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setUserId(order.getUser().getId());
        orderDto.setOrderDate(order.getOrderDate());
        orderDto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        orderDto.setStatus(order.getStatus());
        orderDto.setTotalAmount(order.getTotalAmount());
        orderDto.setShippingAddress(order.getShippingAddress());

        orderDto.setItems(order.getItems().stream()
                .map(item -> {
                   OrderItemDto itemDto = new OrderItemDto();
                    itemDto.setId(item.getId());
                    itemDto.setProductId(item.getProduct().getId());
                    itemDto.setProductName(item.getProduct().getName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setPrice(item.getPrice());
                    itemDto.setSize(ProductSize.valueOf(item.getSize()));
                    itemDto.setColor(item.getColor());
                    return itemDto;
                })
                .collect(Collectors.toList()));

        return orderDto;
    }
}