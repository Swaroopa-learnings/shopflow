package com.shopflow.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SPRING SECURITY configuration for the token-ISSUING service.
 *
 * Modern style (Spring Security 6): declare a SecurityFilterChain bean with
 * the lambda DSL - the old WebSecurityConfigurerAdapter is gone.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection defends session-cookie based browser apps.
                // A stateless JSON API authenticated by header token doesn't use
                // cookies, so CSRF is disabled - know WHY, not just the incantation.
                .csrf(csrf -> csrf.disable())
                // No HttpSession: every request must carry its own credentials.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // registration + login are obviously public
                        .requestMatchers("/api/v1/auth/**", "/actuator/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                // The H2 console renders itself inside HTML frames, but Spring Security
                // sends "X-Frame-Options: DENY" by default (clickjacking protection),
                // which makes the console show a blank page. sameOrigin allows framing
                // by our own host only. Safe here because the console is dev-profile
                // only; never relax this for a real UI.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * BCrypt = adaptive, salted password hashing. Same input never produces the
     * same hash twice (random salt), and the cost factor can be raised as
     * hardware gets faster. NEVER store plaintext or fast hashes (MD5/SHA-x).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
