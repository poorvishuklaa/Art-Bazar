package com.artbazaar.service;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.artwork.ArtworkDetailResponse;
import com.artbazaar.dto.artwork.ArtworkRequest;
import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.entity.Category;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.CategoryRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * An artist managing their own artworks. Every lookup is scoped to the signed-in artist:
 * someone else's artwork answers 404, so ids of other artists' drafts are not revealed.
 */
@Service
@RequiredArgsConstructor
public class ArtistArtworkService {

    private static final String FOLDER = "artworks";

    private final ArtworkRepository artworkRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final FileStorageService storage;

    @Transactional(readOnly = true)
    public PageResponse<ArtworkSummaryResponse> listMine(UserPrincipal me, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, ArtworkCatalogService.MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(artworkRepository.findByArtistIdAndStatusNot(me.id(), ArtworkStatus.REMOVED, pageable),
                ArtworkSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ArtworkDetailResponse getMine(UserPrincipal me, Long id) {
        return detail(owned(me, id));
    }

    @Transactional
    public ArtworkDetailResponse create(UserPrincipal me, ArtworkRequest req, MultipartFile image) {
        Artwork artwork = new Artwork();
        artwork.setArtist(userRepository.getReferenceById(me.id()));
        apply(artwork, req);
        artwork.setImageUrl(storeImage(image));
        return detail(artworkRepository.save(artwork));
    }

    @Transactional
    public ArtworkDetailResponse update(UserPrincipal me, Long id, ArtworkRequest req, MultipartFile image) {
        Artwork artwork = owned(me, id);
        apply(artwork, req);
        if (image != null && !image.isEmpty()) {
            String old = artwork.getImageUrl();
            artwork.setImageUrl(storeImage(image));
            afterCommit(() -> storage.delete(old));
        }
        return detail(artwork);
    }

    /** Quick listed/hidden toggle from the dashboard. */
    @Transactional
    public ArtworkDetailResponse setStatus(UserPrincipal me, Long id, ArtworkStatus status) {
        if (status == ArtworkStatus.REMOVED) {
            throw new BadRequestException("Status must be ACTIVE or HIDDEN");
        }
        Artwork artwork = owned(me, id);
        artwork.setStatus(status);
        return detail(artwork);
    }

    /**
     * Soft delete: the row stays so past orders and reviews keep pointing at it,
     * but it disappears from the shop and from the artist's list.
     */
    @Transactional
    public void delete(UserPrincipal me, Long id) {
        owned(me, id).setStatus(ArtworkStatus.REMOVED);
    }

    private void apply(Artwork artwork, ArtworkRequest req) {
        ArtworkStatus status = req.status() == null ? ArtworkStatus.ACTIVE : req.status();
        if (status == ArtworkStatus.REMOVED) {
            throw new BadRequestException("Status must be ACTIVE or HIDDEN");
        }
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new BadRequestException("Unknown category"));
        artwork.setTitle(req.title().trim());
        artwork.setDescription(StringUtils.hasText(req.description()) ? req.description().trim() : null);
        artwork.setPrice(req.price());
        artwork.setStock(req.stock());
        artwork.setCategory(category);
        artwork.setStatus(status);
    }

    /** Stores the file now, and deletes it again if the surrounding transaction rolls back. */
    private String storeImage(MultipartFile image) {
        String url = storage.storeImage(image, FOLDER);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        storage.delete(url);
                    }
                }
            });
        }
        return url;
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private Artwork owned(UserPrincipal me, Long id) {
        return artworkRepository.findWithDetailsById(id)
                .filter(a -> a.getArtist().getId().equals(me.id()) && a.getStatus() != ArtworkStatus.REMOVED)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork " + id + " not found"));
    }

    private ArtworkDetailResponse detail(Artwork a) {
        String city = artistProfileRepository.findByUserId(a.getArtist().getId()).map(p -> p.getCity()).orElse(null);
        return ArtworkDetailResponse.from(a, city);
    }
}
