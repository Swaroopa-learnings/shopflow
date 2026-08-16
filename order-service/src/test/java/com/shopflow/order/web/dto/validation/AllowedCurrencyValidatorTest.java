package com.shopflow.order.web.dto.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the custom currency constraint. The simplest class in the
 * module - a good warm-up.
 *
 * Worth covering:
 *  - "EUR" and "INR" are accepted too
 *  - lower case ("usd") is accepted
 *  - null is accepted (the constraint does not imply required)
 */
class AllowedCurrencyValidatorTest {

    private final AllowedCurrencyValidator validator = new AllowedCurrencyValidator();

    @Test
    void acceptsASupportedCurrencyAndRejectsAnUnsupportedOne() {
        // the context argument is unused by this validator, so null is fine
        assertThat(validator.isValid("USD", null)).isTrue();
        assertThat(validator.isValid("GBP", null)).isFalse();
    }
}
