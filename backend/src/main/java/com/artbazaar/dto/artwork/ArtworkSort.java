package com.artbazaar.dto.artwork;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

public enum ArtworkSort {
    NEWEST("newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    PRICE_ASC("price_asc", Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"))),
    PRICE_DESC("price_desc", Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"))),
    TITLE("title", Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id")));

    private final String param;
    private final Sort sort;

    ArtworkSort(String param, Sort sort) {
        this.param = param;
        this.sort = sort;
    }

    @JsonValue
    public String param() {
        return param;
    }

    public Sort sort() {
        return sort;
    }

    /** Unknown values fall back to NEWEST rather than failing the request. */
    public static ArtworkSort fromParam(String value) {
        return Arrays.stream(values()).filter(s -> s.param.equalsIgnoreCase(value)).findFirst().orElse(NEWEST);
    }
}
