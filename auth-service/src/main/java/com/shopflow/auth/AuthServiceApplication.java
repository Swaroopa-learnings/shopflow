package com.shopflow.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AUTH-SERVICE - owns user identity and issues JWTs.
 *
 * JWT (JSON Web Token) IN 30 SECONDS:
 *   header.payload.signature (each part Base64Url-encoded)
 *   - payload carries CLAIMS: sub (user id), roles, iat (issued at), exp (expiry)
 *   - signature = HMAC/RSA over header+payload -> any tampering invalidates it
 * The token is STATELESS: any service holding the verification key can check it
 * locally - no session store, no DB lookup per request. That is exactly why
 * JWTs fit microservices: authentication happens once here, verification
 * happens everywhere (gateway + order-service) without calling back.
 *
 * Trade-off to mention in interviews: statelessness means you cannot easily
 * revoke a stolen token before it expires - mitigations are short expiry +
 * refresh tokens, or a Redis denylist checked at the gateway.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
