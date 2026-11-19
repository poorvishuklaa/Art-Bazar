package com.artbazaar.service;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.order.OrderResponse;
import com.artbazaar.dto.sales.ArtistOrderResponse;
import com.artbazaar.dto.sales.SalesSummaryResponse;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderItem;
import com.artbazaar.entity.OrderItemStatus;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ConflictException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.OrderItemRepository;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.artbazaar.repository.OrderItemRepository.toBigDecimal;

/** Fulfilment and sales figures for the signed-in artist. */
@Service
@RequiredArgsConstructor
public class ArtistSalesService {

    /** Sales months are reported in Indian time, so a 1 a.m. IST sale counts in the right month. */
    static final ZoneId SALES_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MONTHS = 6;

    private final OrderItemRepository orderItemRepository;
    private final ArtworkRepository artworkRepository;
    private final Clock clock;

    public enum Filter { TO_SHIP, ALL }

    @Transactional(readOnly = true)
    public PageResponse<ArtistOrderResponse> orders(UserPrincipal me, Filter filter, int page, int size) {
        // Unpaid orders are not the artist's business yet; refunded ones stay visible as history.
        List<PaymentStatus> payment = filter == Filter.TO_SHIP
                ? List.of(PaymentStatus.PAID) : List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED);
        OrderItemStatus itemStatus = filter == Filter.TO_SHIP ? OrderItemStatus.PENDING : null;
        var sort = filter == Filter.TO_SHIP
                ? Sort.by(Sort.Order.asc("paidAt"), Sort.Order.asc("id"))   // oldest first: ship in order
                : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<Order> orders = orderItemRepository.findArtistOrders(me.id(), payment, itemStatus,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), sort));

        Map<Long, List<OrderItem>> mine = orders.isEmpty() ? Map.of() : orderItemRepository
                .findByOrderIdInAndArtistIdOrderByIdAsc(orders.map(Order::getId).toList(), me.id())
                .stream().collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        return PageResponse.of(orders, o -> toResponse(o, mine.getOrDefault(o.getId(), List.of())));
    }

    /**
     * Moves all of this artist's lines in an order forward: PENDING → SHIPPED → DELIVERED.
     * The order's overall status is then derived from every artist's progress.
     */
    @Transactional
    public ArtistOrderResponse updateFulfilment(UserPrincipal me, Long orderId, OrderItemStatus target) {
        if (target != OrderItemStatus.SHIPPED && target != OrderItemStatus.DELIVERED) {
            throw new BadRequestException("Status must be SHIPPED or DELIVERED");
        }
        List<OrderItem> items = orderItemRepository.findByOrderIdAndArtistId(orderId, me.id());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Order " + orderId + " not found");
        }
        Order order = items.getFirst().getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("This order was cancelled");
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ConflictException("This order hasn't been paid yet");
        }
        OrderItemStatus current = combined(items);
        boolean validStep = (current == OrderItemStatus.PENDING && target == OrderItemStatus.SHIPPED)
                || (current == OrderItemStatus.SHIPPED && target == OrderItemStatus.DELIVERED);
        if (!validStep) {
            throw new ConflictException("Can't change from " + label(current) + " to " + label(target));
        }
        items.forEach(i -> i.setStatus(target));
        order.setStatus(deriveOrderStatus(order));
        return toResponse(order, items);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse summary(UserPrincipal me) {
        Instant now = clock.instant();
        Object[] all = orderItemRepository.revenueAndUnits(me.id(), null).getFirst();
        Object[] recent = orderItemRepository.revenueAndUnits(me.id(), now.minus(Duration.ofDays(30))).getFirst();

        // Last 6 calendar months (IST), including months with no sales so the chart has no gaps.
        YearMonth thisMonth = YearMonth.now(clock.withZone(SALES_ZONE));
        YearMonth first = thisMonth.minusMonths(MONTHS - 1);
        Instant since = first.atDay(1).atStartOfDay(SALES_ZONE).toInstant();
        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        // Bucketed here rather than with SQL month() so the month boundary is midnight IST, not UTC.
        for (Object[] row : orderItemRepository.paidLinesSince(me.id(), since)) {
            YearMonth month = YearMonth.from(((Instant) row[0]).atZone(SALES_ZONE));
            byMonth.merge(month, toBigDecimal(row[1]), BigDecimal::add);
        }
        List<SalesSummaryResponse.Month> monthly = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(thisMonth); m = m.plusMonths(1)) {
            monthly.add(new SalesSummaryResponse.Month(m.toString(), byMonth.getOrDefault(m, BigDecimal.ZERO)));
        }

        List<SalesSummaryResponse.TopArtwork> top = orderItemRepository.topArtworks(me.id(), PageRequest.of(0, 5))
                .stream()
                .map(r -> new SalesSummaryResponse.TopArtwork((String) r[0], ((Number) r[1]).longValue(), toBigDecimal(r[2])))
                .toList();

        return new SalesSummaryResponse(toBigDecimal(all[0]), ((Number) all[1]).longValue(),
                toBigDecimal(recent[0]), ((Number) recent[1]).longValue(),
                orderItemRepository.countOrdersToShip(me.id()),
                artworkRepository.countByArtistIdAndStatus(me.id(), ArtworkStatus.ACTIVE),
                monthly, top);
    }

    /** Overall order status from all artists' lines (cancelled lines are ignored). */
    static OrderStatus deriveOrderStatus(Order order) {
        List<OrderItemStatus> live = order.getItems().stream().map(OrderItem::getStatus)
                .filter(s -> s != OrderItemStatus.CANCELLED).toList();
        if (live.isEmpty()) return OrderStatus.CANCELLED;
        if (live.stream().allMatch(s -> s == OrderItemStatus.DELIVERED)) return OrderStatus.DELIVERED;
        if (live.stream().allMatch(s -> s != OrderItemStatus.PENDING)) return OrderStatus.SHIPPED;
        if (live.stream().anyMatch(s -> s != OrderItemStatus.PENDING)) return OrderStatus.PARTIALLY_SHIPPED;
        return OrderStatus.PAID;
    }

    /** One artist's part of an order moves together, so its status is that of its least-advanced line. */
    private static OrderItemStatus combined(List<OrderItem> items) {
        if (items.stream().allMatch(i -> i.getStatus() == OrderItemStatus.CANCELLED)) return OrderItemStatus.CANCELLED;
        return items.stream().map(OrderItem::getStatus).filter(s -> s != OrderItemStatus.CANCELLED)
                .min(Comparator.comparingInt(OrderItemStatus::ordinal)).orElse(OrderItemStatus.PENDING);
    }

    private static String label(OrderItemStatus s) {
        return s.name().toLowerCase();
    }

    private static ArtistOrderResponse toResponse(Order o, List<OrderItem> mine) {
        List<ArtistOrderResponse.Line> lines = mine.stream().map(i -> new ArtistOrderResponse.Line(i.getId(),
                i.getArtwork() == null ? null : i.getArtwork().getId(), i.getArtworkTitle(), i.getImageUrl(),
                i.getUnitPrice(), i.getQuantity(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())),
                i.getStatus())).toList();
        BigDecimal total = lines.stream().map(ArtistOrderResponse.Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        var shipping = new OrderResponse.Shipping(o.getShippingName(), o.getShippingPhone(), o.getShippingAddress(),
                o.getShippingCity(), o.getShippingState(), o.getShippingPincode());
        return new ArtistOrderResponse(o.getId(), o.getCreatedAt(), o.getPaidAt(), o.getStatus(), o.getPaymentStatus(),
                combined(mine), total, shipping, lines);
    }
}
