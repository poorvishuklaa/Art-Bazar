package com.artbazaar.dto.artwork;

import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;

import java.math.BigDecimal;

/** Card-sized view of an artwork, used in lists and grids. */
public record ArtworkSummaryResponse(Long id, String title, BigDecimal price, String imageUrl, int stock,
                                     ArtworkStatus status, CategoryRef category, ArtistRef artist) {

    public static ArtworkSummaryResponse from(Artwork a) {
        return new ArtworkSummaryResponse(a.getId(), a.getTitle(), a.getPrice(), a.getImageUrl(), a.getStock(),
                a.getStatus(), CategoryRef.from(a.getCategory()), ArtistRef.from(a.getArtist(), null));
    }
}
