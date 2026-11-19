package com.artbazaar.controller;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.artwork.ArtworkDetailResponse;
import com.artbazaar.dto.artwork.ArtworkRequest;
import com.artbazaar.dto.artwork.ArtworkStatusRequest;
import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.ArtistArtworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The signed-in artist's own artworks. /api/artist/** already requires ROLE_ARTIST;
 * changing anything additionally requires an admin-approved account.
 */
@Tag(name = "Artist: my artworks")
@RestController
@RequestMapping("/api/artist/artworks")
@RequiredArgsConstructor
public class ArtistArtworkController {

    private static final String APPROVED = "hasAuthority('" + UserPrincipal.ARTIST_APPROVED + "')";

    private final ArtistArtworkService service;

    @Operation(summary = "My artworks, newest first (listed and hidden)")
    @GetMapping
    public PageResponse<ArtworkSummaryResponse> list(@AuthenticationPrincipal UserPrincipal me,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "24") int size) {
        return service.listMine(me, page, size);
    }

    @Operation(summary = "One of my artworks")
    @GetMapping("/{id}")
    public ArtworkDetailResponse get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return service.getMine(me, id);
    }

    @Operation(summary = "Add an artwork (multipart form: fields + image)")
    @PreAuthorize(APPROVED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArtworkDetailResponse create(@AuthenticationPrincipal UserPrincipal me,
                                        @Valid @ModelAttribute ArtworkRequest request,
                                        @RequestPart("image") MultipartFile image) {
        return service.create(me, request, image);
    }

    @Operation(summary = "Edit an artwork; the image part is optional")
    @PreAuthorize(APPROVED)
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArtworkDetailResponse update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                        @Valid @ModelAttribute ArtworkRequest request,
                                        @RequestPart(value = "image", required = false) MultipartFile image) {
        return service.update(me, id, request, image);
    }

    @Operation(summary = "List (ACTIVE) or hide (HIDDEN) an artwork")
    @PreAuthorize(APPROVED)
    @PatchMapping("/{id}/status")
    public ArtworkDetailResponse setStatus(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                           @Valid @RequestBody ArtworkStatusRequest request) {
        return service.setStatus(me, id, request.status());
    }

    @Operation(summary = "Remove an artwork from the shop")
    @PreAuthorize(APPROVED)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        service.delete(me, id);
    }
}
