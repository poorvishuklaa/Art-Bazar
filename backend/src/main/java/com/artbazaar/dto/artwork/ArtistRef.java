package com.artbazaar.dto.artwork;

import com.artbazaar.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArtistRef(Long id, String name, String city) {

    public static ArtistRef from(User artist, String city) {
        return new ArtistRef(artist.getId(), artist.getName(), city);
    }
}
