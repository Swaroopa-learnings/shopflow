package com.shopflow.auth.security;

import com.shopflow.auth.domain.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Mints signed JWTs. Verification happens in api-gateway and order-service
 * using the same shared secret (HS256).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = expiryMinutes;
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                // "sub" claim = stable user identifier; downstream services use it
                // as the customer id (gateway forwards it as X-User-Id).
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                // Short-lived on purpose: a stateless token cannot be revoked,
                // so a small expiry window limits the damage of a leaked token.
                .expiration(Date.from(now.plus(Duration.ofMinutes(expiryMinutes))))
                .signWith(key)
                .compact();
    }
}
