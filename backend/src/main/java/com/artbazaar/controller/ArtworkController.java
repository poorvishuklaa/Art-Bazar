package com.artbazaar.controller;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.artwork.ArtworkDetailResponse;
import com.artbazaar.dto.artwork.ArtworkSort;
import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.service.ArtworkCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Artworks")
@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkCatalogService catalogService;

    @Operation(summary = "Search the public catalog")
    @GetMapping
    public PageResponse<ArtworkSummaryResponse> search(
            @Parameter(description = "Text to match in title or description") @RequestParam(required = false) String q,
            @Parameter(description = "Category slug, e.g. paintings") @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long artistId,
            @Parameter(description = "newest | price_asc | price_desc | title")
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var criteria = new ArtworkCatalogService.SearchCriteria(q, category, minPrice, maxPrice, artistId,
                ArtworkSort.fromParam(sort));
        return catalogService.search(criteria, page, size);
    }

    @Operation(summary = "One listed artwork")
    @GetMapping("/{id}")
    public ArtworkDetailResponse get(@PathVariable Long id) {
        return catalogService.get(id);
    }
}
