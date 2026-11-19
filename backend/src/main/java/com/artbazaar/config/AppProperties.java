package com.artbazaar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed binding for the {@code app.*} section of application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Storage storage) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String uploadDir) {
    }
}
