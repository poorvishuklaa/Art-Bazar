package com.artbazaar.repository;

import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.ArtworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
