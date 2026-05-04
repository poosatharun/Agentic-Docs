package com.agentic.docs.core.ratelimit;

import com.agentic.docs.core.config.AgenticDocsProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter using Bucket4j's in-memory token-bucket algorithm.
 * Each IP gets its own bucket (refilled every 60 s). Buckets are stored in-memory;
 * for clustered deployments swap {@link ConcurrentHashMap} with a Bucket4j-Redis proxy.
 *
 * Configured via {@code agentic.docs.rate-limit.*} in application.properties.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AgenticDocsProperties properties;

    public RateLimiterService(AgenticDocsProperties properties) {
        this.properties = properties;
    }

    /** Returns {@code true} if the request is allowed, {@code false} if rate limit is exceeded. */
    public boolean tryConsume(String clientIp) {
        AgenticDocsProperties.RateLimit config = properties.rateLimit();
        if (!config.enabled()) return true;

        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> buildBucket(config));
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            log.warn("[AgenticDocs] Rate limit exceeded for IP: {} ({} req/min)",
                    clientIp, config.requestsPerMinute());
        }
        return allowed;
    }

    private Bucket buildBucket(AgenticDocsProperties.RateLimit config) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.requestsPerMinute())
                .refillGreedy(config.requestsPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
