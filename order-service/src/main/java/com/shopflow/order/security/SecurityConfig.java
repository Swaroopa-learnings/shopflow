package com.shopflow.order.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Resource-server style security: this service never issues tokens, it only
 * VERIFIES them (via JwtAuthFilter) and enforces authorization rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                       // stateless token API - no cookies
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                // The H2 console renders itself inside HTML frames, but Spring Security
                // sends "X-Frame-Options: DENY" by default (clickjacking protection),
                // which makes the console show a blank page. sameOrigin allows framing
                // by our own host only. Safe here because the console is dev-profile
                // only; never relax this for a real UI.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Insert our JWT filter where username/password auth would normally
                // run - i.e. early enough to authenticate before authorization checks.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
