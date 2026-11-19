package com.artbazaar.security;

import com.artbazaar.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after {@value #MAX_FAILURES} failed logins for the same email from
 * the same IP within {@link #WINDOW}, further attempts are refused until the oldest failure ages out.
 * A successful login clears the count. Kept in memory: fine for a single instance; with several
 * instances this would move to Redis.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String email, String ip) {
        Deque<Instant> recent = failures.get(key(email, ip));
        if (recent == null) return;
        synchronized (recent) {
            prune(recent);
            if (recent.size() >= MAX_FAILURES) {
                long wait = Math.max(1, Duration.between(clock.instant(), recent.peekFirst().plus(WINDOW)).toSeconds());
                long minutes = (wait + 59) / 60;
                throw new TooManyRequestsException("Too many failed sign-in attempts. Try again in " + minutes
                        + (minutes == 1 ? " minute." : " minutes."), wait);
            }
        }
    }

    public void recordFailure(String email, String ip) {
        Deque<Instant> recent = failures.computeIfAbsent(key(email, ip), k -> new ArrayDeque<>());
        synchronized (recent) {
            prune(recent);
            recent.addLast(clock.instant());
        }
    }

    public void recordSuccess(String email, String ip) {
        failures.remove(key(email, ip));
    }

    /** Drop entries whose failures have all expired, so the map can't grow without bound. */
    @Scheduled(fixedDelayString = "PT10M")
    public void evictExpired() {
        failures.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                prune(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }

    private void prune(Deque<Instant> recent) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) recent.pollFirst();
    }

    private static String key(String email, String ip) {
        return email.trim().toLowerCase(Locale.ROOT) + "|" + ip;
    }
}
