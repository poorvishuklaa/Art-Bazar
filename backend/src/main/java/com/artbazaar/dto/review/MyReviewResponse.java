package com.artbazaar.dto.review;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What the current visitor may do: {@code canReview} is true for buyers who paid for the artwork.
 * {@code review} is their existing review, if any.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyReviewResponse(boolean canReview, ReviewResponse review) {
}
