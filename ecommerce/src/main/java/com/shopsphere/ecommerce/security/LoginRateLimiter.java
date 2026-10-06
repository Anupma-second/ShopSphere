package com.shopsphere.ecommerce.security;

import com.shopsphere.ecommerce.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing on /api/auth/login.
 *
 * Counts FAILED logins inside a sliding window (default 15 min):
 *  - per email + IP  (default 5)  -> stops guessing one account's password
 *  - per IP          (default 20) -> stops trying many accounts from one machine
 * A successful login clears the email + IP counter.
 *
 * Kept in memory: fine for one server. With several servers use Redis instead.
 */
@Component
public class LoginRateLimiter {

    private final int maxPerAccount;
    private final int maxPerIp;
    private final Duration window;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.login.max-failures-per-account:5}") int maxPerAccount,
            @Value("${app.login.max-failures-per-ip:20}") int maxPerIp,
            @Value("${app.login.window-minutes:15}") int windowMinutes) {
        this.maxPerAccount = maxPerAccount;
        this.maxPerIp = maxPerIp;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /** Throws 429 if this email/IP has failed too often recently. */
    public void check(String email, String ip) {

        long waitSeconds = Math.max(
                secondsUntilAllowed(accountKey(email, ip), maxPerAccount),
                secondsUntilAllowed(ipKey(ip), maxPerIp));

        if (waitSeconds > 0) {
            long minutes = (waitSeconds + 59) / 60;
            throw new TooManyRequestsException(
                    "Too many failed login attempts. Please try again in "
                            + minutes + (minutes == 1 ? " minute." : " minutes."),
                    waitSeconds);
        }
    }

    public void recordFailure(String email, String ip) {
        Instant now = Instant.now();
        add(accountKey(email, ip), now);
        add(ipKey(ip), now);
    }

    public void recordSuccess(String email, String ip) {
        failures.remove(accountKey(email, ip));
    }

    /** Drops expired entries so the map doesn't grow forever. */
    @Scheduled(fixedDelay = 600_000)   // every 10 min
    public void cleanUp() {
        Instant cutoff = Instant.now().minus(window);
        failures.entrySet().removeIf(entry -> {
            Deque<Instant> times = entry.getValue();
            synchronized (times) {
                prune(times, cutoff);
                return times.isEmpty();
            }
        });
    }

    // ---------------------------------------------------------------------

    private long secondsUntilAllowed(String key, int max) {

        Deque<Instant> times = failures.get(key);
        if (times == null) return 0;

        synchronized (times) {
            prune(times, Instant.now().minus(window));
            if (times.size() < max) return 0;

            // allowed again once the oldest counted failure leaves the window
            Instant unlockAt = times.peekFirst().plus(window);
            return Math.max(1, Duration.between(Instant.now(), unlockAt).toSeconds());
        }
    }

    private void add(String key, Instant time) {
        Deque<Instant> times = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            times.addLast(time);
        }
    }

    private static void prune(Deque<Instant> times, Instant cutoff) {
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.pollFirst();
        }
    }

    private static String accountKey(String email, String ip) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        return "account:" + ip + ":" + normalized;
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }
}
