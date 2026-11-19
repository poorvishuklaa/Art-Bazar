package com.artbazaar.service;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.artwork.ArtistProfileResponse;
import com.artbazaar.dto.artwork.ArtworkDetailResponse;
import com.artbazaar.dto.artwork.ArtworkSort;
import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.ReviewRepository;
import com.artbazaar.dto.review.RatingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.artbazaar.repository.ArtworkSpecifications.byArtist;
import static com.artbazaar.repository.ArtworkSpecifications.inCategory;
import static com.artbazaar.repository.ArtworkSpecifications.matches;
import static com.artbazaar.repository.ArtworkSpecifications.priceAtLeast;
import static com.artbazaar.repository.ArtworkSpecifications.priceAtMost;
import static com.artbazaar.repository.ArtworkSpecifications.visibleToPublic;

/** Read side of the catalog, as buyers and visitors see it. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkCatalogService {

    public static final int MAX_PAGE_SIZE = 48;

    private final ArtworkRepository artworkRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ReviewRepository reviewRepository;

    public record SearchCriteria(String q, String category, BigDecimal minPrice, BigDecimal maxPrice,
                                 Long artistId, ArtworkSort sort) {
    }

    public PageResponse<ArtworkSummaryResponse> search(SearchCriteria c, int page, int size) {
        if (c.minPrice() != null && c.maxPrice() != null && c.minPrice().compareTo(c.maxPrice()) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }
        List<Specification<Artwork>> specs = new ArrayList<>();
        specs.add(visibleToPublic());
        if (StringUtils.hasText(c.q())) specs.add(matches(c.q()));
        if (StringUtils.hasText(c.category())) specs.add(inCategory(c.category()));
        if (c.minPrice() != null) specs.add(priceAtLeast(c.minPrice()));
        if (c.maxPrice() != null) specs.add(priceAtMost(c.maxPrice()));
        if (c.artistId() != null) specs.add(byArtist(c.artistId()));

        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), c.sort().sort());
        var results = artworkRepository.findAll(Specification.allOf(specs), pageable);
        Map<Long, RatingStats> ratings = ratingsFor(results.map(Artwork::getId).toList());
        return PageResponse.of(results, a -> ArtworkSummaryResponse.from(a, ratings.getOrDefault(a.getId(), RatingStats.NONE)));
    }

    private Map<Long, RatingStats> ratingsFor(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, RatingStats> map = new HashMap<>();
        for (Object[] row : reviewRepository.statsFor(ids)) map.put((Long) row[0], RatingStats.of(row[1], row[2]));
        return map;
    }

    public ArtworkDetailResponse get(Long id) {
        Artwork artwork = artworkRepository.findWithDetailsById(id)
                .filter(a -> a.getStatus() == ArtworkStatus.ACTIVE && a.getArtist().isEnabled())
                .orElseThrow(() -> notFound(id));
        ArtistProfile profile = artistProfileRepository.findByUserId(artwork.getArtist().getId())
                .filter(ArtistProfile::isApproved)
                .orElseThrow(() -> notFound(id));
        RatingStats rating = ratingsFor(List.of(id)).getOrDefault(id, RatingStats.NONE);
        return ArtworkDetailResponse.from(artwork, profile.getCity(), rating);
    }

    public ArtistProfileResponse artist(Long userId) {
        ArtistProfile p = artistProfileRepository.findByUserId(userId)
                .filter(ArtistProfile::isApproved)
                .filter(profile -> profile.getUser().isEnabled())
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        long count = artworkRepository.countByArtistIdAndStatus(userId, ArtworkStatus.ACTIVE);
        Object[] stats = reviewRepository.artistStats(userId).getFirst();
        RatingStats rating = RatingStats.of(stats[0], stats[1]);
        return new ArtistProfileResponse(userId, p.getUser().getName(), p.getCity(), p.getBio(), p.getAvatarUrl(),
                count, p.getCreatedAt(), rating.count() == 0 ? null : rating.average(), rating.count());
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Artwork " + id + " not found");
    }
}
