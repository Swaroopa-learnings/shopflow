package com.shopflow.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REQUEST VALIDATION (Bean Validation / JSR-380).
 *
 * Constraints live ON THE DTO; they fire when the controller parameter is
 * annotated with @Valid. Violations raise MethodArgumentNotValidException,
 * which GlobalExceptionHandler converts into a clean 400 response listing
 * every bad field. Validating at the edge keeps garbage out of the domain.
 */
public record RegisterRequest(

        @NotBlank(message = "email is required")
        @Email(message = "must be a valid email address")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password,

        @NotBlank(message = "fullName is required")
        @Size(max = 100)
        String fullName
) {
}
