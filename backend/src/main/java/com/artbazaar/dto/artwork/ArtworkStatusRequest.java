package com.artbazaar.dto.artwork;

import com.artbazaar.entity.ArtworkStatus;
import jakarta.validation.constraints.NotNull;

public record ArtworkStatusRequest(@NotNull(message = "status is required") ArtworkStatus status) {
}
