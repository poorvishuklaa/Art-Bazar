package com.artbazaar.repository;

import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderItem;
import com.artbazaar.entity.OrderItemStatus;
import com.artbazaar.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Order lines seen from the artist's side: an order can mix several artists, each sees only their own lines. */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Orders containing this artist's items, optionally only those with an item in the given status. */
    @Query(value = """
            select distinct o from Order o join o.items i
            where i.artist.id = :artistId and o.paymentStatus in :payment
              and (:itemStatus is null or i.status = :itemStatus)
            """,
            countQuery = """
            select count(distinct o) from Order o join o.items i
            where i.artist.id = :artistId and o.paymentStatus in :payment
              and (:itemStatus is null or i.status = :itemStatus)
            """)
    Page<Order> findArtistOrders(@Param("artistId") Long artistId,
                                 @Param("payment") Collection<PaymentStatus> payment,
                                 @Param("itemStatus") OrderItemStatus itemStatus,
                                 Pageable pageable);

    @EntityGraph(attributePaths = {"order", "artwork"})
    List<OrderItem> findByOrderIdInAndArtistIdOrderByIdAsc(Collection<Long> orderIds, Long artistId);

    @EntityGraph(attributePaths = {"order"})
    List<OrderItem> findByOrderIdAndArtistId(Long orderId, Long artistId);

    // ---- sales figures: count only items from paid orders that were not cancelled ----

    @Query("""
            select coalesce(sum(i.unitPrice * i.quantity), 0), coalesce(sum(i.quantity), 0)
            from OrderItem i
            where i.artist.id = :artistId and i.order.paymentStatus = com.artbazaar.entity.PaymentStatus.PAID
              and i.status <> com.artbazaar.entity.OrderItemStatus.CANCELLED
              and (:since is null or i.order.paidAt >= :since)
            """)
    List<Object[]> revenueAndUnits(@Param("artistId") Long artistId, @Param("since") Instant since);

    @Query("""
            select count(distinct i.order.id) from OrderItem i
            where i.artist.id = :artistId and i.order.paymentStatus = com.artbazaar.entity.PaymentStatus.PAID
              and i.status = com.artbazaar.entity.OrderItemStatus.PENDING
            """)
    long countOrdersToShip(@Param("artistId") Long artistId);

    /** (paidAt, line amount) for each sold line since the cutoff; bucketed into months by the caller. */
    @Query("""
            select i.order.paidAt, i.unitPrice * i.quantity
            from OrderItem i
            where i.artist.id = :artistId and i.order.paymentStatus = com.artbazaar.entity.PaymentStatus.PAID
              and i.status <> com.artbazaar.entity.OrderItemStatus.CANCELLED
              and i.order.paidAt >= :since
            """)
    List<Object[]> paidLinesSince(@Param("artistId") Long artistId, @Param("since") Instant since);

    @Query("""
            select i.artworkTitle, sum(i.quantity), sum(i.unitPrice * i.quantity)
            from OrderItem i
            where i.artist.id = :artistId and i.order.paymentStatus = com.artbazaar.entity.PaymentStatus.PAID
              and i.status <> com.artbazaar.entity.OrderItemStatus.CANCELLED
            group by i.artworkTitle
            order by sum(i.unitPrice * i.quantity) desc
            """)
    List<Object[]> topArtworks(@Param("artistId") Long artistId, Pageable pageable);

    /** Has this buyer paid for this artwork (in an order line that wasn't cancelled)? Required to review it. */
    @Query("""
            select count(i) > 0 from OrderItem i
            where i.artwork.id = :artworkId and i.order.buyer.id = :buyerId
              and i.order.paymentStatus = com.artbazaar.entity.PaymentStatus.PAID
              and i.status <> com.artbazaar.entity.OrderItemStatus.CANCELLED
            """)
    boolean hasPurchased(@Param("buyerId") Long buyerId, @Param("artworkId") Long artworkId);

    static BigDecimal toBigDecimal(Object o) {
        return o instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(o));
    }
}
