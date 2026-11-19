package com.artbazaar.dto.admin;

import com.artbazaar.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserResponse(Long id, String name, String email, Role role, boolean enabled,
                                Boolean artistApproved, Instant joinedAt) {
}
