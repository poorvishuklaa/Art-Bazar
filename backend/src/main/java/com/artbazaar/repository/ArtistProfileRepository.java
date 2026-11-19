package com.artbazaar.repository;

import com.artbazaar.entity.ArtistProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {

    Optional<ArtistProfile> findByUserId(Long userId);

    @Query("select p.user.id from ArtistProfile p where p.approved = true and p.user.id in :userIds")
    Set<Long> findApprovedUserIds(@Param("userIds") Collection<Long> userIds);

    @EntityGraph(attributePaths = "user")
    List<ArtistProfile> findAllByApprovedOrderByCreatedAtAsc(boolean approved);

    @EntityGraph(attributePaths = "user")
    List<ArtistProfile> findAllByOrderByCreatedAtDesc();
}
