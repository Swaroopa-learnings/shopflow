package com.shopflow.auth.web;

import com.shopflow.auth.service.AuthService;
import com.shopflow.auth.web.dto.LoginRequest;
import com.shopflow.auth.web.dto.RegisterRequest;
import com.shopflow.auth.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints:
 *   POST /api/v1/auth/register - create an account, returns a token
 *   POST /api/v1/auth/login    - check credentials, returns a token
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final long expiryMinutes;

    public AuthController(AuthService authService,
                          @Value("${app.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.authService = authService;
        this.expiryMinutes = expiryMinutes;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.bearer(token, expiryMinutes));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(TokenResponse.bearer(token, expiryMinutes));
    }
}
