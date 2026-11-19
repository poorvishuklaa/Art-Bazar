package com.artbazaar.dto.order;

import com.artbazaar.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * Simulated payment. Card details are only checked for shape and never stored or logged.
 * Test outcomes: card 4000 0000 0000 0002 or UPI id fail@upi is declined; anything valid succeeds.
 */
public record PaymentRequest(
        @NotNull(message = "Choose a payment method") PaymentMethod method,
        String upiId,
        String cardNumber,
        String cardExpiry,
        String cardCvv) {

    @Override
    public String toString() {
        return "PaymentRequest[method=" + method + "]"; // never print card data
    }
}
