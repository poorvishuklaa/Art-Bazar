package com.artbazaar.service;

import com.artbazaar.dto.admin.ArtistApplicationResponse;
import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminArtistService {

    private final ArtistProfileRepository artistProfileRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ArtistApplicationResponse> list(Boolean approved) {
        List<ArtistProfile> profiles = approved == null
                ? artistProfileRepository.findAllByOrderByCreatedAtDesc()
                : artistProfileRepository.findAllByApprovedOrderByCreatedAtAsc(approved);
        return profiles.stream().map(ArtistApplicationResponse::from).toList();
    }

    @Transactional
    public ArtistApplicationResponse setApproval(Long userId, boolean approved) {
        ArtistProfile profile = artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No artist with id " + userId));
        profile.setApproved(approved);
        profile.setApprovedAt(approved ? clock.instant() : null);
        return ArtistApplicationResponse.from(profile);
    }
}
