package com.artbazaar.dto;

import com.artbazaar.entity.Category;

public record CategoryResponse(Long id, String name, String slug, String iconUrl) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getIconUrl());
    }
}
