package LLD;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// ============================
// 1. Request Context
// ============================
final class RateLimitRequest {
    private final String clientId;
    private final int tokensRequested;

    public RateLimitRequest(String clientId, int tokensRequested) {
        if (tokensRequested <= 0) {
            throw new IllegalArgumentException("tokensRequested must be > 0");
        }
        this.clientId = Objects.requireNonNull(clientId);
        this.tokensRequested = tokensRequested;
    }

    public String getClientId() {
        return clientId;
    }

    public int getTokensRequested() {
        return tokensRequested;
    }
}

// ============================
// 2. Response Model
// ============================
final class RateLimitResult {
    private final boolean allowed;
    private final long retryAfterMillis;
    private final double remainingTokens;

    public RateLimitResult(boolean allowed, long retryAfterMillis, double remainingTokens) {
        this.allowed = allowed;
        this.retryAfterMillis = retryAfterMillis;
        this.remainingTokens = remainingTokens;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    public double getRemainingTokens() {
        return remainingTokens;
    }

    @Override
    public String toString() {
        return "RateLimitResult{" +
                "allowed=" + allowed +
                ", retryAfterMillis=" + retryAfterMillis +
                ", remainingTokens=" + remainingTokens +
                '}';
    }
}

// ============================
// 3. Contract
// ============================
interface RateLimiter {
    RateLimitResult allow(RateLimitRequest request);
}

// ============================
// 4. Policy Contract
// ============================
interface RateLimitPolicy {
    RateLimitResult allow(String clientId, int tokens);
}

// ============================
// 5. Immutable Config
// ============================
final class RateLimitConfig {
    private final int capacity;
    private final int refillTokens;
    private final long refillIntervalMillis;

    public RateLimitConfig(int capacity, int refillTokens, long refillIntervalMillis) {
        if (capacity <= 0 || refillTokens <= 0 || refillIntervalMillis <= 0) {
            throw new IllegalArgumentException("All config values must be > 0");
        }
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public long getRefillIntervalMillis() {
        return refillIntervalMillis;
    }

    public double getTokensPerMillis() {
        return (double) refillTokens / refillIntervalMillis;
    }
}

// ============================
// 6. Per-client Bucket
// ============================
final class TokenBucket {
    private final int capacity;
    private final double tokensPerMillis;
    private double availableTokens;
    private long lastRefillTimestamp;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(RateLimitConfig config, long currentTimeMillis) {
        this.capacity = config.getCapacity();
        this.tokensPerMillis = config.getTokensPerMillis();
        this.availableTokens = capacity;
        this.lastRefillTimestamp = currentTimeMillis;
    }

    public RateLimitResult tryConsume(int requestedTokens, long nowMillis) {
        lock.lock();
        try {
            refill(nowMillis);

            if (availableTokens >= requestedTokens) {
                availableTokens -= requestedTokens;
                return new RateLimitResult(true, 0, availableTokens);
            }

            double deficit = requestedTokens - availableTokens;
            long retryAfterMillis = (long) Math.ceil(deficit / tokensPerMillis);
            return new RateLimitResult(false, retryAfterMillis, availableTokens);
        } finally {
            lock.unlock();
        }
    }

    private void refill(long nowMillis) {
        if (nowMillis <= lastRefillTimestamp) {
            return;
        }

        long elapsed = nowMillis - lastRefillTimestamp;
        double tokensToAdd = elapsed * tokensPerMillis;
        availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
        lastRefillTimestamp = nowMillis;
    }
}

// ============================
// 7. Token Bucket Policy
// ============================
final class TokenBucketPolicy implements RateLimitPolicy {
    private final RateLimitConfig config;
    private final Clock clock;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketPolicy(RateLimitConfig config, Clock clock) {
        this.config = Objects.requireNonNull(config);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public RateLimitResult allow(String clientId, int tokens) {
        long now = clock.millis();

        TokenBucket bucket = buckets.computeIfAbsent(
                clientId,
                key -> new TokenBucket(config, now));

        return bucket.tryConsume(tokens, now);
    }
}

// ============================
// 8. Rate Limiter Service
// ============================
final class RateLimiterService implements RateLimiter {
    private final RateLimitPolicy policy;

    public RateLimiterService(RateLimitPolicy policy) {
        this.policy = Objects.requireNonNull(policy);
    }

    @Override
    public RateLimitResult allow(RateLimitRequest request) {
        return policy.allow(request.getClientId(), request.getTokensRequested());
    }
}

// ============================
// 9. Demo
// ============================
public class RateLimiterLLD {
    public static void main(String[] args) {
        RateLimitConfig config = new RateLimitConfig(
                10, // bucket capacity
                10, // refill 10 tokens
                1000 // every 1000 ms
        );

        RateLimiter rateLimiter = new RateLimiterService(
                new TokenBucketPolicy(config, Clock.systemUTC()));

        String clientId = "user-123";

        for (int i = 1; i <= 15; i++) {
            RateLimitResult result = rateLimiter.allow(new RateLimitRequest(clientId, 1));
            System.out.println("Request " + i + " at " + Instant.now() + " => " + result);
        }
    }
}
