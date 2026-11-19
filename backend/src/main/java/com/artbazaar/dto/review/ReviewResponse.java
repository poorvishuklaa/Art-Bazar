package com.artbazaar.dto.review;

import com.artbazaar.entity.Review;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** A published review. The reviewer is shown as "First L." so full names and emails stay private. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(Long id, int rating, String comment, String reviewerName, Instant createdAt,
                             Instant updatedAt, boolean edited) {

    public static ReviewResponse from(Review r) {
        boolean edited = r.getUpdatedAt() != null && r.getCreatedAt() != null
                && r.getUpdatedAt().isAfter(r.getCreatedAt().plusSeconds(5));
        return new ReviewResponse(r.getId(), r.getRating(), r.getComment(), displayName(r.getBuyer().getName()),
                r.getCreatedAt(), r.getUpdatedAt(), edited);
    }

    static String displayName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
