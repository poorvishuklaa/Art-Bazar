package com.artbazaar.dto.artwork;

import com.artbazaar.dto.review.RatingStats;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Card-sized view of an artwork, used in lists and grids. Rating fields are present in the public
 * catalog (averageRating only when there is at least one review) and omitted elsewhere.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArtworkSummaryResponse(Long id, String title, BigDecimal price, String imageUrl, int stock,
                                     ArtworkStatus status, CategoryRef category, ArtistRef artist,
                                     Double averageRating, Long reviewCount) {

    public static ArtworkSummaryResponse from(Artwork a) {
        return from(a, null);
    }

    public static ArtworkSummaryResponse from(Artwork a, RatingStats rating) {
        return new ArtworkSummaryResponse(a.getId(), a.getTitle(), a.getPrice(), a.getImageUrl(), a.getStock(),
                a.getStatus(), CategoryRef.from(a.getCategory()), ArtistRef.from(a.getArtist(), null),
                rating == null || rating.count() == 0 ? null : rating.average(),
                rating == null ? null : rating.count());
    }
}
