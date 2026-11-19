package com.artbazaar.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** bio and city are ignored for non-artists. */
public record UpdateProfileRequest(
        @NotBlank(message = "Name is required") @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters") String name,
        @Size(max = 1000, message = "Bio can be at most 1000 characters") String bio,
        @Size(max = 100, message = "City can be at most 100 characters") String city) {
}
