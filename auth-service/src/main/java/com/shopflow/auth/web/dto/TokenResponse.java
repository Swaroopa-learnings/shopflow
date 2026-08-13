package com.shopflow.auth.web.dto;

/** Returned by register and login. The client sends the token back as "Authorization: Bearer ...". */
public record TokenResponse(String token, String tokenType, long expiresInMinutes) {

    public static TokenResponse bearer(String token, long expiresInMinutes) {
        return new TokenResponse(token, "Bearer", expiresInMinutes);
    }
}
