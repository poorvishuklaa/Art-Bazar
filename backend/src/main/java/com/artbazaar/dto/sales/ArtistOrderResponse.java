package com.artbazaar.dto.sales;

import com.artbazaar.dto.order.OrderResponse;
import com.artbazaar.entity.OrderItemStatus;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One order as a particular artist sees it: only their own lines, the shipping address they
 * need to post them, and the combined status of their part.
 */
public record ArtistOrderResponse(Long orderId, Instant createdAt, Instant paidAt, OrderStatus orderStatus,
                                  PaymentStatus paymentStatus, OrderItemStatus myStatus, BigDecimal myTotal,
                                  OrderResponse.Shipping shipping, List<Line> items) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Line(Long id, Long artworkId, String title, String imageUrl, BigDecimal unitPrice, int quantity,
                       BigDecimal lineTotal, OrderItemStatus status) {
    }
}
