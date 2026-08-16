package com.shopflow.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifies the JWT on every request before it is forwarded.
 *
 * Public paths pass straight through. Otherwise the token's signature and
 * expiry are checked, and the user id is added as an X-User-Id header so
 * downstream services don't have to parse the token again.
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    /** Paths that must work without a token. */
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth",     // login + register
            "/actuator"         // gateway's own health endpoints
    );

    private final SecretKey key;

    public JwtAuthenticationGlobalFilter(@Value("${app.jwt.secret}") String secret) {
        // Same secret auth-service signs with; needs at least 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("In JwtAuthenticationGlobalFilter");
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "Missing Authorization: Bearer header");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();

            // Downstream services trust this header because all traffic
            // reaches them through the gateway.
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .build();
            log.info("In JwtAuthenticationGlobalFilter and returning with X_User-Id {} in headers",claims.getSubject());
            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException e) {
            return reject(exchange, "Invalid or expired token: " + e.getMessage());
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.warn("Rejected {} {} -> 401 ({})",
                exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(), reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // Runs before routing so unauthenticated traffic is dropped early.
        return -100;
    }
}
