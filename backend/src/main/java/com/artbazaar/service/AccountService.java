package com.artbazaar.service;

import com.artbazaar.dto.account.AccountResponse;
import com.artbazaar.dto.account.UpdateProfileRequest;
import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import com.artbazaar.exception.UnauthorizedException;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    @Transactional(readOnly = true)
    public AccountResponse get(UserPrincipal me) {
        User user = load(me);
        return toResponse(user, artistProfileRepository.findByUserId(user.getId()).orElse(null));
    }

    @Transactional
    public AccountResponse update(UserPrincipal me, UpdateProfileRequest req) {
        User user = load(me);
        user.setName(req.name().trim());
        ArtistProfile profile = artistProfileRepository.findByUserId(user.getId()).orElse(null);
        if (user.getRole() == Role.ARTIST && profile != null) {
            profile.setBio(StringUtils.hasText(req.bio()) ? req.bio().trim() : null);
            profile.setCity(StringUtils.hasText(req.city()) ? req.city().trim() : null);
        }
        return toResponse(user, profile);
    }

    private User load(UserPrincipal me) {
        return userRepository.findById(me.id()).orElseThrow(() -> new UnauthorizedException("Session expired"));
    }

    private static AccountResponse toResponse(User u, ArtistProfile p) {
        boolean artist = u.getRole() == Role.ARTIST && p != null;
        return new AccountResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt(),
                artist ? p.getBio() : null, artist ? p.getCity() : null, artist ? p.isApproved() : null);
    }
}
