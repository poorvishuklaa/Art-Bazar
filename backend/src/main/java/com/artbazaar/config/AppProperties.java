package com.artbazaar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Typed binding for the {@code app.*} section of application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Admin admin, Seed seed, Orders orders, Cors cors, Storage storage) {

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl, boolean refreshCookieSecure) {
    }

    public record Admin(String email, String password, String name) {
    }

    public record Seed(boolean demoData) {
    }

    /** @param paymentWindow how long stock stays reserved for an unpaid order */
    public record Orders(Duration paymentWindow) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String uploadDir) {
    }
}
