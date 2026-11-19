package com.artbazaar.controller;

import com.artbazaar.dto.admin.ArtistApplicationResponse;
import com.artbazaar.dto.admin.ArtistApprovalRequest;
import com.artbazaar.service.AdminArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@Tag(name = "Admin: artists")
@RestController
@RequestMapping("/api/admin/artists")
@RequiredArgsConstructor
public class AdminArtistController {

    private final AdminArtistService adminArtistService;

    @Operation(summary = "List artists; filter with ?approved=false for pending applications")
    @GetMapping
    public List<ArtistApplicationResponse> list(@RequestParam(required = false) Boolean approved) {
        return adminArtistService.list(approved);
    }

    @Operation(summary = "Approve or revoke an artist")
    @PatchMapping("/{userId}/approval")
    public ArtistApplicationResponse setApproval(@PathVariable Long userId,
                                                 @Valid @RequestBody ArtistApprovalRequest request) {
        return adminArtistService.setApproval(userId, request.approved());
    }
}
