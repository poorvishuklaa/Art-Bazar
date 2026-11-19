package com.artbazaar.controller;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.order.CheckoutRequest;
import com.artbazaar.dto.order.OrderResponse;
import com.artbazaar.dto.order.OrderSummaryResponse;
import com.artbazaar.dto.order.PaymentRequest;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place an order from the cart (reserves stock; pay within the payment window)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody CheckoutRequest req) {
        return orderService.checkout(me, req);
    }

    @Operation(summary = "Pay for an order (simulated). Test declines: card 4000 0000 0000 0002, UPI fail@upi")
    @PostMapping("/{id}/pay")
    public OrderResponse pay(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
                             @Valid @RequestBody PaymentRequest req) {
        return orderService.pay(me, id, req);
    }

    @Operation(summary = "Cancel an order that hasn't shipped (stock is returned, payment refunded)")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return orderService.cancel(me, id);
    }

    @Operation(summary = "My orders, newest first")
    @GetMapping
    public PageResponse<OrderSummaryResponse> list(@AuthenticationPrincipal UserPrincipal me,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return orderService.list(me, page, size);
    }

    @Operation(summary = "One of my orders")
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return orderService.get(me, id);
    }
}
