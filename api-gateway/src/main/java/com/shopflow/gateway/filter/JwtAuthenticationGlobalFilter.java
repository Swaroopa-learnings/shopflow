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
 * EDGE AUTHENTICATION - a reactive {@link GlobalFilter} that runs for EVERY
 * route and verifies the JWT before the request is forwarded downstream.
 *
 * FLOW:
 *  1. Public paths (login/register, actuator) are whitelisted and pass through.
 *  2. Otherwise the "Authorization: Bearer &lt;token&gt;" header is required.
 *  3. The token signature + expiry are verified with the shared HMAC secret.
 *  4. On success we MUTATE the request and add "X-User-Id" so downstream
 *     services know who the caller is without re-parsing the token themselves.
 *
 * DEFENSE IN DEPTH: order-service *also* validates the JWT (see its
 * SecurityConfig). The gateway check gives fast rejection at the edge; the
 * service check protects against anyone who bypasses the gateway on the
 * internal network. In a service mesh, mTLS between sidecars adds a third layer.
 *
 * INTERVIEW NOTE: HS256 (shared secret) keeps the demo simple, but it means
 * every verifier could also MINT tokens. Production systems prefer RS256:
 * auth-service signs with a private key, everyone else verifies with the
 * public key (fetched from a JWKS endpoint).
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    /** Paths that must work WITHOUT a token. */
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth",     // login + register
            "/actuator"         // gateway's own health endpoints
    );

    private final SecretKey key;

    public JwtAuthenticationGlobalFilter(@Value("${app.jwt.secret}") String secret) {
        // Same secret as auth-service; must be >= 32 bytes for HS256.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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

            // Propagate identity downstream as a plain header. Services trust this
            // header only because all traffic is forced through the gateway.
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .build();
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
        // Run BEFORE the rate limiter / routing filters so unauthenticated
        // traffic is dropped as early as possible.
        return -100;
    }
}
