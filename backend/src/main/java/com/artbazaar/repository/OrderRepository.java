package com.artbazaar.repository;

import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.artist", "items.artwork"})
    Optional<Order> findWithItemsByIdAndBuyerId(Long id, Long buyerId);

    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    @EntityGraph(attributePaths = {"buyer"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"buyer"})
    Page<Order> findAll(Pageable pageable);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByStatusIn(Collection<OrderStatus> statuses);

    @Query("""
            select coalesce(sum(o.totalAmount), 0), count(o) from Order o
            where o.paymentStatus = com.artbazaar.entity.PaymentStatus.PAID and (:since is null or o.paidAt >= :since)
            """)
    List<Object[]> paidTotals(@Param("since") Instant since);

    /** Orders that were placed but never paid, older than the cutoff. */
    List<Order> findByStatusAndPaymentStatusInAndCreatedAtBefore(OrderStatus status,
                                                                 Collection<PaymentStatus> paymentStatuses,
                                                                 Instant cutoff);
}
