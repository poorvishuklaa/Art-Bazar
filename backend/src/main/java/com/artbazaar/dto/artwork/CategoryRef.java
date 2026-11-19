package com.artbazaar.dto.artwork;

import com.artbazaar.entity.Category;

public record CategoryRef(Long id, String name, String slug) {

    public static CategoryRef from(Category c) {
        return new CategoryRef(c.getId(), c.getName(), c.getSlug());
    }
}
