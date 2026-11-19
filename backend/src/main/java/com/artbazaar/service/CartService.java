package com.artbazaar.service;

import com.artbazaar.dto.artwork.ArtworkSummaryResponse;
import com.artbazaar.dto.cart.CartResponse;
import com.artbazaar.dto.cart.CartResponse.Issue;
import com.artbazaar.dto.cart.CartResponse.Line;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.CartItem;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ConflictException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.CartItemRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ArtworkRepository artworkRepository;
    private final UserRepository userRepository;
    private final PurchasableArtworks purchasable;

    @Transactional(readOnly = true)
    public CartResponse view(UserPrincipal me) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtAsc(me.id());
        Set<Long> approved = purchasable.approvedArtistIds(items.stream().map(CartItem::getArtwork).toList());

        List<Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int count = 0;
        for (CartItem item : items) {
            Artwork a = item.getArtwork();
            Issue issue = issueFor(a, item.getQuantity(), approved);
            BigDecimal lineTotal = a.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            if (issue == null) {
                subtotal = subtotal.add(lineTotal);
                count += item.getQuantity();
            }
            lines.add(new Line(ArtworkSummaryResponse.from(a), item.getQuantity(), lineTotal, issue));
        }
        boolean ready = !lines.isEmpty() && lines.stream().allMatch(l -> l.issue() == null);
        return new CartResponse(lines, count, subtotal, ready);
    }

    @Transactional
    public CartResponse add(UserPrincipal me, Long artworkId, int quantity) {
        Artwork artwork = purchasableArtwork(me, artworkId);
        CartItem item = cartItemRepository.findByUserIdAndArtworkId(me.id(), artworkId).orElseGet(() -> {
            CartItem c = new CartItem();
            c.setUser(userRepository.getReferenceById(me.id()));
            c.setArtwork(artwork);
            return c;
        });
        int wanted = item.getQuantity() + quantity;
        checkStock(artwork, wanted);
        item.setQuantity(wanted);
        cartItemRepository.save(item);
        return view(me);
    }

    @Transactional
    public CartResponse update(UserPrincipal me, Long artworkId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndArtworkId(me.id(), artworkId)
                .orElseThrow(() -> new ResourceNotFoundException("That artwork is not in your cart"));
        checkStock(item.getArtwork(), quantity);
        item.setQuantity(quantity);
        return view(me);
    }

    @Transactional
    public CartResponse remove(UserPrincipal me, Long artworkId) {
        cartItemRepository.findByUserIdAndArtworkId(me.id(), artworkId).ifPresent(cartItemRepository::delete);
        return view(me);
    }

    @Transactional
    public void clear(UserPrincipal me) {
        cartItemRepository.deleteAllForUser(me.id());
    }

    private Artwork purchasableArtwork(UserPrincipal me, Long artworkId) {
        Artwork artwork = artworkRepository.findWithDetailsById(artworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork " + artworkId + " not found"));
        if (!purchasable.isListed(artwork, purchasable.approvedArtistIds(List.of(artwork)))) {
            throw new ResourceNotFoundException("Artwork " + artworkId + " not found");
        }
        if (artwork.getArtist().getId().equals(me.id())) {
            throw new BadRequestException("You can't buy your own artwork");
        }
        return artwork;
    }

    private static void checkStock(Artwork artwork, int wanted) {
        if (artwork.getStock() == 0) {
            throw new ConflictException("“" + artwork.getTitle() + "” is sold out");
        }
        if (wanted > artwork.getStock()) {
            throw new ConflictException("Only " + artwork.getStock() + " of “" + artwork.getTitle() + "” available");
        }
    }

    private Issue issueFor(Artwork a, int quantity, Set<Long> approvedArtists) {
        if (!purchasable.isListed(a, approvedArtists)) return Issue.UNAVAILABLE;
        if (a.getStock() == 0) return Issue.SOLD_OUT;
        if (quantity > a.getStock()) return Issue.NOT_ENOUGH_STOCK;
        return null;
    }
}
