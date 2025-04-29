package com.veststore.veststoreback.dto;

import com.veststore.veststoreback.model.ProductSize;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal price;
    private ProductSize size;
    private String color;
}