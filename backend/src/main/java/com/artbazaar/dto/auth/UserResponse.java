package com.artbazaar.dto.auth;

import com.artbazaar.entity.Role;
import com.artbazaar.security.UserPrincipal;
import com.fasterxml.jackson.annotation.JsonInclude;

/** The signed-in user. {@code artistApproved} is only present for artists. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(Long id, String name, String email, Role role, Boolean artistApproved) {

    public static UserResponse from(UserPrincipal p) {
        return new UserResponse(p.id(), p.name(), p.email(), p.role(),
                p.role() == Role.ARTIST ? p.artistApproved() : null);
    }
}
