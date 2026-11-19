package com.artbazaar.controller;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.admin.AdminArtworkStatusRequest;
import com.artbazaar.dto.admin.AdminCategoryResponse;
import com.artbazaar.dto.admin.AdminOrderResponse;
import com.artbazaar.dto.admin.AdminStatsResponse;
import com.artbazaar.dto.admin.AdminUserResponse;
import com.artbazaar.dto.admin.CategoryRequest;
import com.artbazaar.dto.admin.UserStatusRequest;
import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.Role;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.AdminCatalogService;
import com.artbazaar.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.util.List;

/** Admin-only (SecurityConfig restricts /api/admin/** to ROLE_ADMIN). Artist approval lives in AdminArtistController. */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminCatalogService catalogService;

    @Operation(summary = "Platform overview")
    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    // ---- users ----

    @Operation(summary = "Search users by name or email, optionally by role")
    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) Role role,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return adminService.users(role, q, page, size);
    }

    @Operation(summary = "Disable or re-enable a buyer or artist account")
    @PatchMapping("/users/{id}/status")
    public AdminUserResponse setUserStatus(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                                           @Valid @RequestBody UserStatusRequest req) {
        return adminService.setEnabled(me, id, req.enabled());
    }

    // ---- artworks ----

    @Operation(summary = "All artworks for moderation (REMOVED only when status=REMOVED)")
    @GetMapping("/artworks")
    public PageResponse<ArtworkSummaryResponse> artworks(@RequestParam(required = false) ArtworkStatus status,
                                                         @RequestParam(required = false) String q,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return catalogService.artworks(status, q, page, size);
    }

    @Operation(summary = "Hide, take down (REMOVED) or restore (ACTIVE) an artwork")
    @PatchMapping("/artworks/{id}/status")
    public ArtworkSummaryResponse setArtworkStatus(@PathVariable Long id, @Valid @RequestBody AdminArtworkStatusRequest req) {
        return catalogService.setArtworkStatus(id, req.status());
    }

    // ---- categories ----

    @Operation(summary = "Categories with their artwork counts")
    @GetMapping("/categories")
    public List<AdminCategoryResponse> categories() {
        return catalogService.categories();
    }

    @Operation(summary = "Create a category (multipart: name + optional icon)")
    @PostMapping(path = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCategoryResponse createCategory(@Valid @ModelAttribute CategoryRequest req,
                                                @RequestPart(value = "icon", required = false) MultipartFile icon) {
        return catalogService.createCategory(req.name(), icon);
    }

    @Operation(summary = "Rename a category and/or replace its icon")
    @PutMapping(path = "/categories/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCategoryResponse updateCategory(@PathVariable Long id, @Valid @ModelAttribute CategoryRequest req,
                                                @RequestPart(value = "icon", required = false) MultipartFile icon) {
        return catalogService.updateCategory(id, req.name(), icon);
    }

    @Operation(summary = "Delete an empty category")
    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
    }

    // ---- orders ----

    @Operation(summary = "All orders, newest first, optionally by status")
    @GetMapping("/orders")
    public PageResponse<AdminOrderResponse> orders(@RequestParam(required = false) OrderStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return adminService.orders(status, page, size);
    }
}
