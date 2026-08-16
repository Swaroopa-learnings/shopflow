package com.shopflow.order.exception;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for exception-to-response mapping.
 *
 * Each handler takes an exception and returns a ResponseEntity, so these can be
 * called directly - no MVC infrastructure needed.
 *
 * Worth covering:
 *  - MethodArgumentNotValidException -> 400, error VALIDATION_FAILED, one entry
 *    per rejected field
 *  - MissingRequestHeaderException   -> 400 naming the missing header
 *  - FeignException with any other status -> 502 (a downstream failure is not our 500)
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void productNotFoundDownstreamIsReportedAs404() {
        // mocking FeignException avoids building a full Request/Response pair
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);

        ResponseEntity<Map<String, Object>> response = handler.handleFeign(notFound);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Product not found");
    }
}
