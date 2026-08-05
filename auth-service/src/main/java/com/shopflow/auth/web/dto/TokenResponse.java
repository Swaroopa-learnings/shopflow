package com.shopflow.auth.web.dto;

/** Returned by login/register; the client sends the token back as "Authorization: Bearer <token>". */
public record TokenResponse(String token, String tokenType, long expiresInMinutes) {

    public static TokenResponse bearer(String token, long expiresInMinutes) {
        return new TokenResponse(token, "Bearer", expiresInMinutes);
    }
}
