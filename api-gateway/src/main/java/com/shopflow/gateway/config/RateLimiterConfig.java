package com.shopflow.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * RATE LIMITING - key resolution strategy.
 *
 * Spring Cloud Gateway's RequestRateLimiter filter (wired per-route in
 * application.yml) implements a TOKEN BUCKET in Redis:
 *   - replenishRate  = tokens added per second (steady-state requests/sec)
 *   - burstCapacity  = bucket size (short bursts allowed above steady rate)
 * When the bucket for a key is empty the gateway answers 429 Too Many Requests.
 *
 * WHY REDIS? The gateway may run as N replicas; keeping counters in Redis makes
 * the limit GLOBAL across all replicas instead of per-instance.
 *
 * The {@link KeyResolver} decides WHOSE bucket a request draws from:
 * here, the authenticated user id (set by JwtAuthenticationGlobalFilter),
 * falling back to client IP for public endpoints. Per-user limiting is fairer
 * than per-IP (corporate NATs share one IP) - a good trade-off to discuss.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                    .getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }
}
