package com.shopflow.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT SPRING SECURITY integration - the standard recipe:
 *
 *  1. This filter runs early in the security chain (see SecurityConfig).
 *  2. It parses/verifies the Bearer token.
 *  3. On success it populates the thread-bound SecurityContextHolder with an
 *     Authentication object -> Spring Security now considers the request
 *     authenticated, roles become available for @PreAuthorize etc.
 *  4. On failure it simply DOESN'T populate the context; the authorization
 *     rules in SecurityConfig then reject the request with 401/403.
 *
 * DEFENSE IN DEPTH: the gateway already verified this token, but a service
 * must not blindly trust its network - anything that reaches this port
 * directly (a misconfigured firewall, a compromised pod) still hits this check.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(header.substring(7))
                        .getPayload();

                String role = claims.get("role", String.class);
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),                       // principal = user id
                        null,                                      // no credentials kept
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ignored) {
                // Invalid token -> leave context empty; SecurityConfig will 401.
            }
        }
        chain.doFilter(request, response);
    }
}
