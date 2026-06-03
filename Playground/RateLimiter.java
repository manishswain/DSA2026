package Playground;

import java.util.*;
import java.util.concurrent.*;

public class RateLimiter {

    private static class RequestWindow {
        // timestamps (millis) of recent requests within the window
        Deque<Long> timestamps = new ArrayDeque<>();
    }

    private final int maxRequests;
    private final long windowInMillis;
    private final ConcurrentHashMap<String, RequestWindow> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowInMillis) {
        this.maxRequests = maxRequests;
        this.windowInMillis = windowInMillis;
    }

    public boolean allowRequest(String clientId, long nowMillis) {
        RequestWindow window = buckets.computeIfAbsent(clientId, id -> new RequestWindow());

        synchronized (window) {
            long boundary = nowMillis - windowInMillis;

            // Drop all timestamps older than the window
            while (!window.timestamps.isEmpty() && window.timestamps.peekFirst() < boundary) {
                window.timestamps.pollFirst();
            }

            if (window.timestamps.size() < maxRequests) {
                window.timestamps.addLast(nowMillis);
                return true;
            } else {
                return false;
            }
        }
    }

    // small demo
    public static void main(String[] args) throws InterruptedException {
        RateLimiter limiter = new RateLimiter(3, 1000);

        String client = "clientA";
        long base = System.currentTimeMillis();

        System.out.println(limiter.allowRequest(client, base)); // true
        System.out.println(limiter.allowRequest(client, base + 10)); // true
        System.out.println(limiter.allowRequest(client, base + 20)); // true
        System.out.println(limiter.allowRequest(client, base + 30)); // false (4th within 1s)

        Thread.sleep(1000);
        long later = System.currentTimeMillis();
        System.out.println(limiter.allowRequest(client, later)); // true (window moved)
    }
}