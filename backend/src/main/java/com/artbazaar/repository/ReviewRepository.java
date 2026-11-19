package com.artbazaar.repository;

import com.artbazaar.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByArtworkIdAndBuyerId(Long artworkId, Long buyerId);

    @EntityGraph(attributePaths = {"buyer"})
    Page<Review> findByArtworkId(Long artworkId, Pageable pageable);

    /** (artworkId, average, count) for every artwork in the list that has reviews: one query per page of cards. */
    @Query("select r.artwork.id, avg(r.rating), count(r) from Review r where r.artwork.id in :ids group by r.artwork.id")
    List<Object[]> statsFor(@Param("ids") Collection<Long> artworkIds);

    /** (rating, count) for one artwork. */
    @Query("select r.rating, count(r) from Review r where r.artwork.id = :id group by r.rating")
    List<Object[]> distribution(@Param("id") Long artworkId);

    /** (average, count) across all of an artist's artworks. */
    @Query("select avg(r.rating), count(r) from Review r where r.artwork.artist.id = :artistId")
    List<Object[]> artistStats(@Param("artistId") Long artistId);
}
