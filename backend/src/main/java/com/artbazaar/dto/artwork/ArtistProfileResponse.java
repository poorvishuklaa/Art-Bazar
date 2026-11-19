package com.artbazaar.dto.artwork;

import java.time.Instant;

/** Public artist page header. averageRating covers all of the artist's reviewed artworks (null if none). */
public record ArtistProfileResponse(Long id, String name, String city, String bio, String avatarUrl,
                                    long artworkCount, Instant memberSince, Double averageRating, long reviewCount) {
}
