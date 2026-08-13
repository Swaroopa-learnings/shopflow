package com.shopflow.product.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/** Body for creating or updating a product. */
public record ProductRequest(

        @NotBlank(message = "name is required")
        @Size(max = 120)
        String name,

        @Size(max = 2000)
        String description,

        @NotBlank(message = "category is required")
        String category,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        BigDecimal price,

        Map<String, String> attributes
) {
}
