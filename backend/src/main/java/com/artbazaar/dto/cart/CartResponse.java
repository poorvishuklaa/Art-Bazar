package com.artbazaar.dto.cart;

import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * The cart as the buyer should see it now. Items that can no longer be bought as-is carry an
 * {@code issue}; they stay in the cart (so the buyer notices) but are excluded from the subtotal.
 */
public record CartResponse(List<Line> items, int itemCount, BigDecimal subtotal, boolean readyForCheckout) {

    public enum Issue {
        /** Hidden, removed, or its artist is no longer approved. */
        UNAVAILABLE,
        SOLD_OUT,
        /** Fewer left than the quantity in the cart. */
        NOT_ENOUGH_STOCK
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Line(ArtworkSummaryResponse artwork, int quantity, BigDecimal lineTotal, Issue issue) {
    }
}
