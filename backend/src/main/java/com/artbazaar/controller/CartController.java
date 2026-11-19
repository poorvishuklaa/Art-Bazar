package com.artbazaar.controller;

import com.artbazaar.dto.cart.AddToCartRequest;
import com.artbazaar.dto.cart.CartResponse;
import com.artbazaar.dto.cart.UpdateCartItemRequest;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in buyer's cart (buyers and artists may shop; enforced in SecurityConfig). */
@Tag(name = "Cart")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "My cart, with availability issues flagged per line")
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal UserPrincipal me) {
        return cartService.view(me);
    }

    @Operation(summary = "Add an artwork (adds to the quantity if it is already in the cart)")
    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody AddToCartRequest req) {
        return cartService.add(me, req.artworkId(), req.quantity());
    }

    @Operation(summary = "Set the quantity of a cart line")
    @PutMapping("/items/{artworkId}")
    public CartResponse update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long artworkId,
                               @Valid @RequestBody UpdateCartItemRequest req) {
        return cartService.update(me, artworkId, req.quantity());
    }

    @Operation(summary = "Remove a cart line")
    @DeleteMapping("/items/{artworkId}")
    public CartResponse remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long artworkId) {
        return cartService.remove(me, artworkId);
    }

    @Operation(summary = "Empty the cart")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal UserPrincipal me) {
        cartService.clear(me);
    }
}
