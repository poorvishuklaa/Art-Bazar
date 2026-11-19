package com.artbazaar.repository;

import com.artbazaar.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"artwork", "artwork.artist", "artwork.category"})
    List<CartItem> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<CartItem> findByUserIdAndArtworkId(Long userId, Long artworkId);

    @Modifying
    @Query("delete from CartItem c where c.user.id = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
