package com.artbazaar.dto.review;

/** Average rating (rounded to one decimal) and number of reviews. */
public record RatingStats(double average, long count) {

    public static final RatingStats NONE = new RatingStats(0, 0);

    public static RatingStats of(Object avg, Object count) {
        long n = count == null ? 0 : ((Number) count).longValue();
        if (n == 0 || avg == null) return NONE;
        return new RatingStats(Math.round(((Number) avg).doubleValue() * 10) / 10.0, n);
    }
}
