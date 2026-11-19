package com.artbazaar.service;

import com.artbazaar.config.AppProperties;
import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.order.CheckoutRequest;
import com.artbazaar.dto.order.OrderResponse;
import com.artbazaar.dto.order.OrderSummaryResponse;
import com.artbazaar.dto.order.PaymentRequest;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.CartItem;
import com.artbazaar.entity.Order;
import com.artbazaar.entity.OrderItem;
import com.artbazaar.entity.OrderItemStatus;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ConflictException;
import com.artbazaar.exception.PaymentDeclinedException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.CartItemRepository;
import com.artbazaar.repository.OrderRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checkout, simulated payment and cancellation.
 *
 * Stock is reserved when the order is placed (not when it is paid), so a buyer who reaches the
 * payment page is guaranteed the items. Unpaid orders give their stock back after the payment window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.PLACED, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ArtworkRepository artworkRepository;
    private final UserRepository userRepository;
    private final PurchasableArtworks purchasable;
    private final PaymentSimulator paymentSimulator;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public OrderResponse checkout(UserPrincipal me, CheckoutRequest req) {
        List<CartItem> cart = cartItemRepository.findByUserIdOrderByCreatedAtAsc(me.id());
        if (cart.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        // Lock every artwork in the cart (SELECT ... FOR UPDATE) so concurrent checkouts are serialised
        // and the stock we check below is the stock we decrement.
        Map<Long, Artwork> locked = artworkRepository
                .lockAllByIdIn(cart.stream().map(c -> c.getArtwork().getId()).toList())
                .stream().collect(Collectors.toMap(Artwork::getId, Function.identity()));
        Set<Long> approved = purchasable.approvedArtistIds(locked.values());

        List<String> problems = new ArrayList<>();
        for (CartItem c : cart) {
            Artwork a = locked.get(c.getArtwork().getId());
            if (a == null || !purchasable.isListed(a, approved)) {
                problems.add("“" + c.getArtwork().getTitle() + "” is no longer available");
            } else if (a.getArtist().getId().equals(me.id())) {
                problems.add("You can't buy your own artwork “" + a.getTitle() + "”");
            } else if (a.getStock() == 0) {
                problems.add("“" + a.getTitle() + "” is sold out");
            } else if (a.getStock() < c.getQuantity()) {
                problems.add("Only " + a.getStock() + " of “" + a.getTitle() + "” left");
            }
        }
        if (!problems.isEmpty()) {
            throw new ConflictException(String.join(". ", problems) + ". Please update your cart.");
        }

        Order order = new Order();
        order.setBuyer(userRepository.getReferenceById(me.id()));
        order.setShippingName(req.name().trim());
        order.setShippingPhone(req.phone().trim());
        order.setShippingAddress(req.address().trim());
        order.setShippingCity(req.city().trim());
        order.setShippingState(req.state().trim());
        order.setShippingPincode(req.pincode().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem c : cart) {
            Artwork a = locked.get(c.getArtwork().getId());
            a.setStock(a.getStock() - c.getQuantity());

            OrderItem item = new OrderItem();
            item.setArtwork(a);
            item.setArtist(a.getArtist());
            item.setArtworkTitle(a.getTitle());
            item.setImageUrl(a.getImageUrl());
            item.setUnitPrice(a.getPrice()); // price at the moment of purchase
            item.setQuantity(c.getQuantity());
            order.addItem(item);
            total = total.add(a.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }
        order.setTotalAmount(total);
        orderRepository.save(order);
        cartItemRepository.deleteAllForUser(me.id());
        log.info("Order {} placed by user {} for {}", order.getId(), me.id(), total);
        return toResponse(order);
    }

    /** A decline is recorded on the order and must survive the exception, hence noRollbackFor. */
    @Transactional(noRollbackFor = PaymentDeclinedException.class)
    public OrderResponse pay(UserPrincipal me, Long orderId, PaymentRequest req) {
        Order order = own(me, orderId);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("This order is already paid");
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ConflictException("This order was cancelled and can no longer be paid");
        }
        order.setPaymentMethod(req.method());
        switch (paymentSimulator.charge(req)) {
            case PaymentSimulator.Approved ok -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaymentReference(ok.reference());
                order.setPaidAt(clock.instant());
                order.setStatus(OrderStatus.PAID);
                log.info("Order {} paid ({})", order.getId(), ok.reference());
            }
            case PaymentSimulator.Declined no -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                throw new PaymentDeclinedException(no.reason());
            }
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(UserPrincipal me, Long orderId) {
        Order order = own(me, orderId);
        if (!isCancellable(order)) {
            throw new ConflictException("This order can no longer be cancelled");
        }
        cancelAndRestock(order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UserPrincipal me, Long orderId) {
        return toResponse(own(me, orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(UserPrincipal me, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(orderRepository.findByBuyerId(me.id(), pageable), OrderSummaryResponse::from);
    }

    /** Every 5 minutes: release the stock held by orders that were never paid. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    @Transactional
    public int expireUnpaidOrders() {
        Instant cutoff = clock.instant().minus(props.orders().paymentWindow());
        List<Order> stale = orderRepository.findByStatusAndPaymentStatusInAndCreatedAtBefore(
                OrderStatus.PLACED, List.of(PaymentStatus.PENDING, PaymentStatus.FAILED), cutoff);
        stale.forEach(this::cancelAndRestock);
        if (!stale.isEmpty()) {
            log.info("Cancelled {} unpaid orders older than {}", stale.size(), props.orders().paymentWindow());
        }
        return stale.size();
    }

    private void cancelAndRestock(Order order) {
        List<Long> ids = order.getItems().stream()
                .filter(i -> i.getArtwork() != null).map(i -> i.getArtwork().getId()).toList();
        Map<Long, Artwork> locked = ids.isEmpty() ? Map.of() : artworkRepository.lockAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Artwork::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            if (item.getArtwork() != null) {
                Artwork a = locked.get(item.getArtwork().getId());
                a.setStock(a.getStock() + item.getQuantity());
            }
            item.setStatus(OrderItemStatus.CANCELLED);
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED); // simulated refund
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(clock.instant());
    }

    /** Buyers can cancel until an artist has shipped any part of the order. */
    private static boolean isCancellable(Order o) {
        return CANCELLABLE.contains(o.getStatus())
                && o.getItems().stream().allMatch(i -> i.getStatus() == OrderItemStatus.PENDING);
    }

    private Order own(UserPrincipal me, Long orderId) {
        return orderRepository.findWithItemsByIdAndBuyerId(orderId, me.id())
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
    }

    private OrderResponse toResponse(Order o) {
        Instant payBy = o.getStatus() == OrderStatus.PLACED && o.getPaymentStatus() != PaymentStatus.PAID
                ? o.getCreatedAt().plus(props.orders().paymentWindow()) : null;
        return OrderResponse.from(o, isCancellable(o), payBy);
    }
}
