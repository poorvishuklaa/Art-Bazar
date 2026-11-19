package com.artbazaar.service;

import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** The single definition of "a buyer may purchase this artwork right now", shared by cart and checkout. */
@Component
@RequiredArgsConstructor
public class PurchasableArtworks {

    private final ArtistProfileRepository artistProfileRepository;

    /** Ids of the given artworks' artists who are approved (one query for a whole cart). */
    public Set<Long> approvedArtistIds(Collection<Artwork> artworks) {
        Set<Long> artistIds = artworks.stream().map(a -> a.getArtist().getId()).collect(Collectors.toSet());
        return artistIds.isEmpty() ? Set.of() : artistProfileRepository.findApprovedUserIds(artistIds);
    }

    public boolean isListed(Artwork a, Set<Long> approvedArtistIds) {
        return a.getStatus() == ArtworkStatus.ACTIVE
                && a.getArtist().isEnabled()
                && approvedArtistIds.contains(a.getArtist().getId());
    }
}
