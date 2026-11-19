package com.artbazaar.dto.sales;

import java.math.BigDecimal;
import java.util.List;

public record SalesSummaryResponse(BigDecimal totalRevenue, long itemsSold, BigDecimal revenueLast30Days,
                                   long itemsSoldLast30Days, long ordersToShip, long activeListings,
                                   List<Month> monthly, List<TopArtwork> topArtworks) {

    /** @param month "2026-11" */
    public record Month(String month, BigDecimal revenue) {
    }

    public record TopArtwork(String title, long quantity, BigDecimal revenue) {
    }
}
