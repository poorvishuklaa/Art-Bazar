package com.artbazaar.controller;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.sales.ArtistOrderResponse;
import com.artbazaar.dto.sales.FulfilmentRequest;
import com.artbazaar.dto.sales.SalesSummaryResponse;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.ArtistSalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Orders and sales for the signed-in artist (/api/artist/** requires ROLE_ARTIST). Approval is
 * not required here: an artist whose approval was revoked must still be able to ship what they sold.
 */
@Tag(name = "Artist: orders & sales")
@RestController
@RequestMapping("/api/artist")
@RequiredArgsConstructor
public class ArtistSalesController {

    private final ArtistSalesService service;

    @Operation(summary = "Orders containing my artworks: filter=to_ship (oldest paid first) or all")
    @GetMapping("/orders")
    public PageResponse<ArtistOrderResponse> orders(@AuthenticationPrincipal UserPrincipal me,
                                                    @RequestParam(defaultValue = "to_ship") String filter,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        var f = "all".equalsIgnoreCase(filter) ? ArtistSalesService.Filter.ALL : ArtistSalesService.Filter.TO_SHIP;
        return service.orders(me, f, page, size);
    }

    @Operation(summary = "Mark my part of an order as SHIPPED, then DELIVERED")
    @PatchMapping("/orders/{orderId}/fulfilment")
    public ArtistOrderResponse fulfil(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long orderId,
                                      @Valid @RequestBody FulfilmentRequest req) {
        return service.updateFulfilment(me, orderId, req.status());
    }

    @Operation(summary = "Revenue, units, orders to ship, last 6 months and top artworks")
    @GetMapping("/sales/summary")
    public SalesSummaryResponse summary(@AuthenticationPrincipal UserPrincipal me) {
        return service.summary(me);
    }
}
