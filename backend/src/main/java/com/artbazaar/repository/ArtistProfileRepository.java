package com.artbazaar.repository;

import com.artbazaar.entity.ArtistProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {

    Optional<ArtistProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<ArtistProfile> findAllByApprovedOrderByCreatedAtAsc(boolean approved);

    @EntityGraph(attributePaths = "user")
    List<ArtistProfile> findAllByOrderByCreatedAtDesc();
}
