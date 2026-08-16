package com.shopflow.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JWT verification.
 *
 * Worth covering:
 *  - no Authorization header -> context stays empty, chain still proceeds
 *  - a header that is not "Bearer ..." -> ignored
 *  - a token signed with a different secret -> context stays empty (no exception thrown)
 *  - an expired token -> context stays empty (build one with an expiration in the past)
 */
class JwtAuthFilterTest {

    /** Must be at least 32 bytes for HS256. */
    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough!!";

    private final JwtAuthFilter filter = new JwtAuthFilter(TEST_SECRET);

    @AfterEach
    void clearSecurityContext() {
        // the context is thread-bound and would leak into the next test
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesTheRequestWithItsSubjectAndRole() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "CUSTOMER")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("42");            // the subject becomes the principal
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }
}
