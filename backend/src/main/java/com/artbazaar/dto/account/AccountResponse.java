package com.artbazaar.dto.account;

import com.artbazaar.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** The signed-in user's own profile; bio and city only for artists. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(Long id, String name, String email, Role role, Instant memberSince,
                              String bio, String city, Boolean artistApproved) {
}
