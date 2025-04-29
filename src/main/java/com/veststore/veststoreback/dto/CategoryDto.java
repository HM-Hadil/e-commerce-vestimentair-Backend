package com.veststore.veststoreback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class CategoryDto {

    private Long id;

    @NotBlank(message = "Category name is required")
    private String name;
}