package com.veststore.veststoreback.dto;
import com.veststore.veststoreback.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class OrderStatusUpdateDto {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}