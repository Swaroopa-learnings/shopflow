package com.shopflow.order.web.dto;

import com.shopflow.order.web.dto.validation.AllowedCurrency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * REQUEST VALIDATION - standard constraints plus a CUSTOM one.
 *
 * @Min/@Max guard business bounds; @AllowedCurrency is a hand-written
 * constraint (see the validation package) showing how to add rules Bean
 * Validation doesn't ship with.
 */
public record CreateOrderRequest(

        @NotBlank(message = "productId is required")
        String productId,

        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 100, message = "quantity cannot exceed 100 per order")
        int quantity,

        @AllowedCurrency   // custom constraint: must be one of USD/EUR/INR
        String currency
) {
}
