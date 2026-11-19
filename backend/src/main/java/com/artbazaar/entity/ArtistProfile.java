package com.artbazaar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "artist_profiles")
public class ArtistProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 100)
    private String city;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** New artists cannot list artworks until an admin approves them. */
    @Column(nullable = false)
    private boolean approved = false;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
