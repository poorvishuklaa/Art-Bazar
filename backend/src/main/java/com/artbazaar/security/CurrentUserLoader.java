package com.artbazaar.security;

import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Loads the live user record for each authenticated request, so disabling a user or
 * approving an artist takes effect immediately rather than when the token expires.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserLoader {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    @Transactional(readOnly = true)
    public Optional<UserPrincipal> loadActive(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .map(user -> UserPrincipal.of(user, isApprovedArtist(user)));
    }

    public boolean isApprovedArtist(User user) {
        return user.getRole() == Role.ARTIST
                && artistProfileRepository.findByUserId(user.getId()).map(ArtistProfile::isApproved).orElse(false);
    }
}
