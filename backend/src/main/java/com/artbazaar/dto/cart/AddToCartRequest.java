package com.artbazaar.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
        @NotNull(message = "artworkId is required") Long artworkId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 20, message = "You can add at most 20 at a time")
        Integer quantity) {
}
