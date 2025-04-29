package com.veststore.veststoreback.dto;

import com.veststore.veststoreback.model.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDto {

    private Long id;
    private Long userId;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDeliveryDate;
    private OrderStatus status;
    private List<OrderItemDto> items = new ArrayList<>();
    private BigDecimal totalAmount;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
}
