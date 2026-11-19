package com.artbazaar.service;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.admin.AdminCategoryResponse;
import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.entity.Category;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ConflictException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.ArtworkSpecifications;
import com.artbazaar.repository.CategoryRepository;
import com.artbazaar.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Admin moderation of artworks and management of categories. */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final ArtworkRepository artworkRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService storage;

    // ---------- artworks ----------

    /** All artworks regardless of listing state; REMOVED ones only when asked for explicitly. */
    @Transactional(readOnly = true)
    public PageResponse<ArtworkSummaryResponse> artworks(ArtworkStatus status, String q, int page, int size) {
        List<Specification<Artwork>> specs = new ArrayList<>();
        specs.add(status == null
                ? (root, query, cb) -> cb.notEqual(root.get("status"), ArtworkStatus.REMOVED)
                : (root, query, cb) -> cb.equal(root.get("status"), status));
        if (StringUtils.hasText(q)) specs.add(ArtworkSpecifications.matches(q));
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(artworkRepository.findAll(Specification.allOf(specs), pageable), ArtworkSummaryResponse::from);
    }

    @Transactional
    public ArtworkSummaryResponse setArtworkStatus(Long id, ArtworkStatus status) {
        Artwork a = artworkRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork " + id + " not found"));
        a.setStatus(status);
        return ArtworkSummaryResponse.from(a);
    }

    // ---------- categories ----------

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> categories() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : artworkRepository.countLiveByCategory()) counts.put((Long) row[0], (Long) row[1]);
        return categoryRepository.findAllByOrderByIdAsc().stream()
                .map(c -> AdminCategoryResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList();
    }

    @Transactional
    public AdminCategoryResponse createCategory(String name, MultipartFile icon) {
        Category c = new Category();
        rename(c, name);
        if (icon != null && !icon.isEmpty()) c.setIconUrl(storage.storeImage(icon, "categories"));
        categoryRepository.save(c);
        return AdminCategoryResponse.from(c, 0);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(Long id, String name, MultipartFile icon) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        rename(c, name);
        if (icon != null && !icon.isEmpty()) {
            String old = c.getIconUrl();
            c.setIconUrl(storage.storeImage(icon, "categories"));
            storage.delete(old);
        }
        return AdminCategoryResponse.from(c, artworkRepository.countByCategoryId(id));
    }

    /** Only empty categories can be deleted, so no artwork (or past order) ever loses its category. */
    @Transactional
    public void deleteCategory(Long id) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        long used = artworkRepository.countByCategoryId(id);
        if (used > 0) {
            throw new ConflictException("“" + c.getName() + "” still has " + used + " artwork" + (used == 1 ? "" : "s")
                    + ". Move or remove them first.");
        }
        categoryRepository.delete(c);
        storage.delete(c.getIconUrl());
    }

    private void rename(Category c, String rawName) {
        String name = rawName.trim();
        String slug = slugify(name);
        if (slug.isEmpty()) throw new BadRequestException("Name must contain letters or numbers");
        categoryRepository.findBySlug(slug).filter(other -> !other.getId().equals(c.getId())).ifPresent(other -> {
            throw new ConflictException("A category called “" + other.getName() + "” already exists");
        });
        c.setName(name);
        c.setSlug(slug);
    }

    /** "Home Décor & Gifts" -> "home-decor-gifts" */
    static String slugify(String s) {
        String ascii = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
