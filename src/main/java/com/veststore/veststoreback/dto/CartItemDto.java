package com.veststore.veststoreback.dto;


import com.veststore.veststoreback.model.CartStatus;
import com.veststore.veststoreback.model.ProductSize;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Cart Items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {

    private Long id;

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String productName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Size is required")
    private ProductSize size;

    @NotNull(message = "Color is required")
    private String color;

    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.EN_ATTENTE;

}