package com.artbazaar.dto.review;

import com.artbazaar.dto.PageResponse;

import java.util.Map;

/** Reviews of one artwork: the summary (with count per star, 5 to 1) plus a page of reviews. */
public record ReviewsPageResponse(double average, long count, Map<Integer, Long> distribution,
                                  PageResponse<ReviewResponse> reviews) {
}
