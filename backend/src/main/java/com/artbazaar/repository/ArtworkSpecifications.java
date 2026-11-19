package com.artbazaar.repository;

import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

/** Composable filters for artwork search. */
public final class ArtworkSpecifications {

    private ArtworkSpecifications() {
    }

    /** Listed, and its artist is enabled and approved: what buyers are allowed to see. */
    public static Specification<Artwork> visibleToPublic() {
        return (root, query, cb) -> {
            Subquery<Long> approved = query.subquery(Long.class);
            var profile = approved.from(ArtistProfile.class);
            approved.select(profile.get("id"))
                    .where(cb.equal(profile.get("user"), root.get("artist")), cb.isTrue(profile.get("approved")));
            return cb.and(
                    cb.equal(root.get("status"), ArtworkStatus.ACTIVE),
                    cb.isTrue(root.get("artist").get("enabled")),
                    cb.exists(approved));
        };
    }

    public static Specification<Artwork> inCategory(String slug) {
        return (root, query, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    public static Specification<Artwork> byArtist(Long artistId) {
        return (root, query, cb) -> cb.equal(root.get("artist").get("id"), artistId);
    }

    public static Specification<Artwork> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Artwork> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    /** Case-insensitive match on title or description. */
    public static Specification<Artwork> matches(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'));
    }

    /** Stops user input like "100%" or "_" acting as SQL wildcards. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
