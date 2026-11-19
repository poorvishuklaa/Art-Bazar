package com.artbazaar.dto.artwork;

import com.artbazaar.dto.review.RatingStats;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;

import java.math.BigDecimal;
import java.time.Instant;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ArtworkDetailResponse(Long id, String title, String description, BigDecimal price, String imageUrl,
                                    int stock, ArtworkStatus status, CategoryRef category, ArtistRef artist,
                                    Instant createdAt, Instant updatedAt, Double averageRating, long reviewCount) {

    public static ArtworkDetailResponse from(Artwork a, String artistCity) {
        return from(a, artistCity, RatingStats.NONE);
    }

    public static ArtworkDetailResponse from(Artwork a, String artistCity, RatingStats rating) {
        return new ArtworkDetailResponse(a.getId(), a.getTitle(), a.getDescription(), a.getPrice(), a.getImageUrl(),
                a.getStock(), a.getStatus(), CategoryRef.from(a.getCategory()), ArtistRef.from(a.getArtist(), artistCity),
                a.getCreatedAt(), a.getUpdatedAt(), rating.count() == 0 ? null : rating.average(), rating.count());
    }
}
