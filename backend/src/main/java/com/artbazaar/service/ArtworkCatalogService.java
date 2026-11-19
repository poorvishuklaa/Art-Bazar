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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        return PageResponse.of(artworkRepository.findAll(Specification.allOf(specs), pageable),
                ArtworkSummaryResponse::from);
    }

    public ArtworkDetailResponse get(Long id) {
        Artwork artwork = artworkRepository.findWithDetailsById(id)
                .filter(a -> a.getStatus() == ArtworkStatus.ACTIVE && a.getArtist().isEnabled())
                .orElseThrow(() -> notFound(id));
        ArtistProfile profile = artistProfileRepository.findByUserId(artwork.getArtist().getId())
                .filter(ArtistProfile::isApproved)
                .orElseThrow(() -> notFound(id));
        return ArtworkDetailResponse.from(artwork, profile.getCity());
    }

    public ArtistProfileResponse artist(Long userId) {
        ArtistProfile p = artistProfileRepository.findByUserId(userId)
                .filter(ArtistProfile::isApproved)
                .filter(profile -> profile.getUser().isEnabled())
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));
        long count = artworkRepository.countByArtistIdAndStatus(userId, ArtworkStatus.ACTIVE);
        return new ArtistProfileResponse(userId, p.getUser().getName(), p.getCity(), p.getBio(), p.getAvatarUrl(),
                count, p.getCreatedAt());
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Artwork " + id + " not found");
    }
}
