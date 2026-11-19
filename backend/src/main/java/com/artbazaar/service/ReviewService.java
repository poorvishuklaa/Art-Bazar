package com.artbazaar.service;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.review.MyReviewResponse;
import com.artbazaar.dto.review.RatingStats;
import com.artbazaar.dto.review.ReviewRequest;
import com.artbazaar.dto.review.ReviewResponse;
import com.artbazaar.dto.review.ReviewsPageResponse;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.Review;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.OrderItemRepository;
import com.artbazaar.repository.ReviewRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Verified-purchase reviews: only buyers who paid for an artwork can rate it, once each. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ArtworkRepository artworkRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ReviewsPageResponse list(Long artworkId, int page, int size) {
        requireArtwork(artworkId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) distribution.put(star, 0L);
        long count = 0;
        long sum = 0;
        for (Object[] row : reviewRepository.distribution(artworkId)) {
            int star = ((Number) row[0]).intValue();
            long n = ((Number) row[1]).longValue();
            distribution.put(star, n);
            count += n;
            sum += star * n;
        }
        RatingStats stats = RatingStats.of(count == 0 ? null : (double) sum / count, count);
        var reviews = reviewRepository.findByArtworkId(artworkId, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 20),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new ReviewsPageResponse(stats.average(), stats.count(), distribution,
                PageResponse.of(reviews, ReviewResponse::from));
    }

    /** Anonymous visitors (me == null) simply can't review. */
    @Transactional(readOnly = true)
    public MyReviewResponse mine(UserPrincipal me, Long artworkId) {
        if (me == null) return new MyReviewResponse(false, null);
        requireArtwork(artworkId);
        var existing = reviewRepository.findByArtworkIdAndBuyerId(artworkId, me.id()).map(ReviewResponse::from).orElse(null);
        return new MyReviewResponse(existing != null || orderItemRepository.hasPurchased(me.id(), artworkId), existing);
    }

    /** Creates the buyer's review, or updates it if they already wrote one. */
    @Transactional
    public ReviewResponse upsert(UserPrincipal me, Long artworkId, ReviewRequest req) {
        Artwork artwork = requireArtwork(artworkId);
        Review review = reviewRepository.findByArtworkIdAndBuyerId(artworkId, me.id()).orElse(null);
        if (review == null) {
            if (!orderItemRepository.hasPurchased(me.id(), artworkId)) {
                throw new AccessDeniedException("Only buyers who purchased this artwork can review it");
            }
            review = new Review();
            review.setArtwork(artwork);
            review.setBuyer(userRepository.getReferenceById(me.id()));
        }
        review.setRating(req.rating());
        review.setComment(StringUtils.hasText(req.comment()) ? req.comment().trim() : null);
        reviewRepository.saveAndFlush(review);
        return ReviewResponse.from(reviewRepository.findById(review.getId()).orElseThrow());
    }

    @Transactional
    public void deleteMine(UserPrincipal me, Long artworkId) {
        reviewRepository.findByArtworkIdAndBuyerId(artworkId, me.id()).ifPresent(reviewRepository::delete);
    }

    /** Moderation: admins can remove any review. */
    @Transactional
    public void deleteAny(Long reviewId) {
        Review r = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(r);
    }

    private Artwork requireArtwork(Long id) {
        return artworkRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Artwork " + id + " not found"));
    }
}
