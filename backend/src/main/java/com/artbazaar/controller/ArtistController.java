package com.artbazaar.controller;

import com.artbazaar.dto.artwork.ArtistProfileResponse;
import com.artbazaar.service.ArtworkCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Artists")
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtworkCatalogService catalogService;

    @Operation(summary = "Public profile of an approved artist (their artworks: GET /api/artworks?artistId=)")
    @GetMapping("/{id}")
    public ArtistProfileResponse get(@PathVariable Long id) {
        return catalogService.artist(id);
    }
}
