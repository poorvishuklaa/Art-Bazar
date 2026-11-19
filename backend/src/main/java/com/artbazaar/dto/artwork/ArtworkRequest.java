package com.artbazaar.dto.artwork;

import com.artbazaar.entity.ArtworkStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Form fields of the multipart create/update request; the image travels as a separate "image" part. */
public record ArtworkRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 150, message = "Title must be 3 to 150 characters")
        String title,

        @Size(max = 5000, message = "Description can be at most 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "1", message = "Price must be at least ₹1")
        @DecimalMax(value = "10000000", message = "Price can be at most ₹1,00,00,000")
        @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        @Max(value = 10000, message = "Stock can be at most 10000")
        Integer stock,

        @NotNull(message = "Choose a category")
        Long categoryId,

        /** ACTIVE (listed) or HIDDEN (draft). Defaults to ACTIVE. */
        ArtworkStatus status) {
}
