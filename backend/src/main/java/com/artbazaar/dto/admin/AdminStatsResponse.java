package com.artbazaar.dto.admin;

import java.math.BigDecimal;

/** Platform overview for the admin home page. GMV = value of all paid orders. */
public record AdminStatsResponse(long buyers, long artists, long pendingArtists, long listedArtworks,
                                 long paidOrders, BigDecimal gmv, long paidOrdersLast30Days,
                                 BigDecimal gmvLast30Days, long ordersAwaitingShipment) {
}
