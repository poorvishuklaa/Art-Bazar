package com.artbazaar.dto.admin;

import com.artbazaar.entity.ArtworkStatus;
import jakarta.validation.constraints.NotNull;

/** HIDDEN = temporarily hide from the shop, REMOVED = take down for good, ACTIVE = restore. */
public record AdminArtworkStatusRequest(@NotNull(message = "status is required") ArtworkStatus status) {
}
