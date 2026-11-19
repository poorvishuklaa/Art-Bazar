package com.artbazaar.security;

import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The authenticated user stored in the SecurityContext.
 * Authorities: ROLE_BUYER / ROLE_ARTIST / ROLE_ADMIN, plus ARTIST_APPROVED for artists an admin has approved.
 */
public record UserPrincipal(Long id, String email, String name, Role role, boolean artistApproved) {

    public static final String ARTIST_APPROVED = "ARTIST_APPROVED";

    public static UserPrincipal of(User user, boolean artistApproved) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getName(), user.getRole(), artistApproved);
    }

    public Collection<GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (role == Role.ARTIST && artistApproved) {
            authorities.add(new SimpleGrantedAuthority(ARTIST_APPROVED));
        }
        return authorities;
    }
}
