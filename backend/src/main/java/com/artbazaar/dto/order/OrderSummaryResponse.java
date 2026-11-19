package com.artbazaar.dto.order;

import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of "My orders". */
public record OrderSummaryResponse(Long id, OrderStatus status, PaymentStatus paymentStatus, BigDecimal totalAmount,
                                   int itemCount, String firstItemTitle, String firstItemImageUrl, Instant createdAt) {

    public static OrderSummaryResponse from(Order o) {
        var first = o.getItems().isEmpty() ? null : o.getItems().getFirst();
        int count = o.getItems().stream().mapToInt(i -> i.getQuantity()).sum();
        return new OrderSummaryResponse(o.getId(), o.getStatus(), o.getPaymentStatus(), o.getTotalAmount(), count,
                first == null ? null : first.getArtworkTitle(), first == null ? null : first.getImageUrl(),
                o.getCreatedAt());
    }
}
