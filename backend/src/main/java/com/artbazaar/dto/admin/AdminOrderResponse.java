package com.artbazaar.dto.admin;

import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderResponse(Long id, String buyerName, String buyerEmail, OrderStatus status,
                                 PaymentStatus paymentStatus, BigDecimal totalAmount, int itemCount,
                                 Instant createdAt) {

    public static AdminOrderResponse from(Order o) {
        int count = o.getItems().stream().mapToInt(i -> i.getQuantity()).sum();
        return new AdminOrderResponse(o.getId(), o.getBuyer().getName(), o.getBuyer().getEmail(), o.getStatus(),
                o.getPaymentStatus(), o.getTotalAmount(), count, o.getCreatedAt());
    }
}
