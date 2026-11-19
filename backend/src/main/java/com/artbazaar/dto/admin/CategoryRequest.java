package com.artbazaar.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Multipart form field; the optional "icon" image travels as a separate part. */
public record CategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 60, message = "Name must be 2 to 60 characters")
        String name) {
}
