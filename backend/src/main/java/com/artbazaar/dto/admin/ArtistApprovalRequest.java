package com.artbazaar.dto.admin;

import jakarta.validation.constraints.NotNull;

public record ArtistApprovalRequest(@NotNull(message = "approved is required") Boolean approved) {
}
