package com.artbazaar.dto.artwork;

import java.time.Instant;

/** Public artist page header. */
public record ArtistProfileResponse(Long id, String name, String city, String bio, String avatarUrl,
                                    long artworkCount, Instant memberSince) {
}
