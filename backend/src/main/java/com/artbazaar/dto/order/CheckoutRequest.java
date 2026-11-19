package com.artbazaar.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Shipping details; everything else (items, prices) comes from the server-side cart. */
public record CheckoutRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a 10-digit Indian mobile number")
        String phone,
        @NotBlank(message = "Address is required") @Size(max = 500) String address,
        @NotBlank(message = "City is required") @Size(max = 100) String city,
        @NotBlank(message = "State is required") @Size(max = 100) String state,
        @NotBlank(message = "PIN code is required")
        @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code")
        String pincode) {
}
