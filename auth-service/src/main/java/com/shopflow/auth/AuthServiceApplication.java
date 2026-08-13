package com.shopflow.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth service: owns user accounts and issues JWTs.
 *
 * Tokens are stateless, so other services verify them locally with the shared
 * key instead of calling back here. Nothing is stored server-side, which is
 * why tokens are short-lived - they can't be revoked before they expire.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
