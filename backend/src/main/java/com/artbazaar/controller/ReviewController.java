package com.artbazaar.controller;

import com.artbazaar.dto.review.MyReviewResponse;
import com.artbazaar.dto.review.ReviewRequest;
import com.artbazaar.dto.review.ReviewResponse;
import com.artbazaar.dto.review.ReviewsPageResponse;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * GETs are public (SecurityConfig permits GET /api/artworks/**); writing requires sign-in, and the
 * service additionally requires a paid purchase of the artwork.
 */
@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Reviews of an artwork with average, count and per-star distribution")
    @GetMapping("/api/artworks/{artworkId}/reviews")
    public ReviewsPageResponse list(@PathVariable Long artworkId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "5") int size) {
        return reviewService.list(artworkId, page, size);
    }

    @Operation(summary = "Whether I can review this artwork, and my existing review")
    @GetMapping("/api/artworks/{artworkId}/reviews/me")
    public MyReviewResponse mine(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long artworkId) {
        return reviewService.mine(me, artworkId);
    }

    @Operation(summary = "Write or update my review (requires having bought the artwork)")
    @PutMapping("/api/artworks/{artworkId}/reviews/me")
    public ReviewResponse upsert(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long artworkId,
                                 @Valid @RequestBody ReviewRequest req) {
        return reviewService.upsert(me, artworkId, req);
    }

    @Operation(summary = "Delete my review")
    @DeleteMapping("/api/artworks/{artworkId}/reviews/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMine(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long artworkId) {
        reviewService.deleteMine(me, artworkId);
    }

    @Operation(summary = "Admin: remove any review")
    @DeleteMapping("/api/admin/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAny(@PathVariable Long reviewId) {
        reviewService.deleteAny(reviewId);
    }
}
