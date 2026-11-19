package com.artbazaar.dto.order;

import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderItem;
import com.artbazaar.entity.OrderItemStatus;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentMethod;
import com.artbazaar.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(Long id, OrderStatus status, PaymentStatus paymentStatus, PaymentMethod paymentMethod,
                            String paymentReference, BigDecimal totalAmount, int itemCount, Shipping shipping,
                            List<Item> items, Instant createdAt, Instant paidAt, Instant cancelledAt,
                            boolean cancellable, Instant payBy) {

    public record Shipping(String name, String phone, String address, String city, String state, String pincode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(Long id, Long artworkId, String title, String imageUrl, BigDecimal unitPrice, int quantity,
                       BigDecimal lineTotal, OrderItemStatus status, Long artistId, String artistName) {

        static Item from(OrderItem i) {
            return new Item(i.getId(), i.getArtwork() == null ? null : i.getArtwork().getId(), i.getArtworkTitle(),
                    i.getImageUrl(), i.getUnitPrice(), i.getQuantity(),
                    i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())), i.getStatus(),
                    i.getArtist().getId(), i.getArtist().getName());
        }
    }

    /** @param payBy deadline for paying an unpaid order, or null when not applicable */
    public static OrderResponse from(Order o, boolean cancellable, Instant payBy) {
        List<Item> items = o.getItems().stream().map(Item::from).toList();
        int count = items.stream().mapToInt(Item::quantity).sum();
        return new OrderResponse(o.getId(), o.getStatus(), o.getPaymentStatus(), o.getPaymentMethod(),
                o.getPaymentReference(), o.getTotalAmount(), count,
                new Shipping(o.getShippingName(), o.getShippingPhone(), o.getShippingAddress(), o.getShippingCity(),
                        o.getShippingState(), o.getShippingPincode()),
                items, o.getCreatedAt(), o.getPaidAt(), o.getCancelledAt(), cancellable, payBy);
    }
}
