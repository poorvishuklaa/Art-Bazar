package com.artbazaar.dto.admin;

import com.artbazaar.entity.ArtistProfile;

import java.time.Instant;

public record ArtistApplicationResponse(
        Long userId, String name, String email, String city, String bio,
        boolean approved, boolean enabled, Instant registeredAt, Instant approvedAt) {

    public static ArtistApplicationResponse from(ArtistProfile p) {
        return new ArtistApplicationResponse(p.getUser().getId(), p.getUser().getName(), p.getUser().getEmail(),
                p.getCity(), p.getBio(), p.isApproved(), p.getUser().isEnabled(), p.getCreatedAt(), p.getApprovedAt());
    }
}
