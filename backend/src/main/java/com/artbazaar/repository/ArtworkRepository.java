package com.artbazaar.repository;

import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {

    /** Category and artist are fetched in the same query, so a page of results is one SELECT, not 1 + 2N. */
    @Override
    @EntityGraph(attributePaths = {"category", "artist"})
    Page<Artwork> findAll(Specification<Artwork> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "artist"})
    Optional<Artwork> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"category", "artist"})
    Page<Artwork> findByArtistIdAndStatusNot(Long artistId, ArtworkStatus status, Pageable pageable);

    long countByArtistIdAndStatus(Long artistId, ArtworkStatus status);

    long countByStatus(ArtworkStatus status);

    long countByCategoryId(Long categoryId);

    @Query("select a.category.id, count(a) from Artwork a where a.status <> com.artbazaar.entity.ArtworkStatus.REMOVED group by a.category.id")
    List<Object[]> countLiveByCategory();

    /**
     * SELECT ... FOR UPDATE on the given artworks, in id order so that two checkouts locking
     * overlapping sets always lock in the same order and cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Artwork a join fetch a.artist where a.id in :ids order by a.id")
    List<Artwork> lockAllByIdIn(@Param("ids") Collection<Long> ids);
}
