package com.artbazaar.repository;

import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.artist", "items.artwork"})
    Optional<Order> findWithItemsByIdAndBuyerId(Long id, Long buyerId);

    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    /** Orders that were placed but never paid, older than the cutoff. */
    List<Order> findByStatusAndPaymentStatusInAndCreatedAtBefore(OrderStatus status,
                                                                 Collection<PaymentStatus> paymentStatuses,
                                                                 Instant cutoff);
}
