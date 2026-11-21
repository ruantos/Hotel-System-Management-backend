package com.hotelmanagement.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.hotelmanagement.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ENTRIES = 10000;

    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;

    public PasswordResetRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void check(String operation, String remoteAddress, int limit) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        String key = operation + ":" + remoteAddress;
        Window current = windows.get(key);
        if (current == null) {
            if (windows.size() >= MAX_ENTRIES) {
                throw new RateLimitExceededException(60);
            }
            windows.put(key, new Window(1, now.plus(WINDOW)));
            return;
        }
        if (current.attempts() >= limit) {
            throw new RateLimitExceededException(Math.max(1, Duration.between(now, current.expiresAt()).toSeconds() + 1));
        }
        windows.put(key, new Window(current.attempts() + 1, current.expiresAt()));
    }

    private record Window(int attempts, Instant expiresAt) {
    }
}
