package com.veststore.veststoreback.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime orderDate = LocalDateTime.now();

    private LocalDateTime estimatedDeliveryDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.EN_ATTENTE;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String shippingAddress;

    public boolean canCancel() {
        return status == OrderStatus.EN_ATTENTE || status == OrderStatus.VALIDEE;
    }

    public boolean canValidate() {
        return status == OrderStatus.EN_ATTENTE;
    }

    public boolean canShip() {
        return status == OrderStatus.VALIDEE;
    }

    public boolean canDeliver() {
        return status == OrderStatus.EXPEDIEE;
    }

    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}