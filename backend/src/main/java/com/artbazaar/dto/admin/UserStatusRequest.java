package com.artbazaar.dto.admin;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull(message = "enabled is required") Boolean enabled) {
}
