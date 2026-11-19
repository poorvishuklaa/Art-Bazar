package com.artbazaar.dto.admin;

import com.artbazaar.entity.Category;

public record AdminCategoryResponse(Long id, String name, String slug, String iconUrl, long artworkCount) {

    public static AdminCategoryResponse from(Category c, long artworkCount) {
        return new AdminCategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getIconUrl(), artworkCount);
    }
}
