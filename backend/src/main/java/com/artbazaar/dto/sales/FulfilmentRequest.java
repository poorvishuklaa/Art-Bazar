package com.artbazaar.dto.sales;

import com.artbazaar.entity.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

/** SHIPPED or DELIVERED: applies to all of this artist's lines in the order. */
public record FulfilmentRequest(@NotNull(message = "status is required") OrderItemStatus status) {
}
