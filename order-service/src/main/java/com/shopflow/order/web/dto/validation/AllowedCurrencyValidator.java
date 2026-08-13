package com.shopflow.order.web.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/** Implements @AllowedCurrency. Null passes - add @NotNull to require a value. */
public class AllowedCurrencyValidator implements ConstraintValidator<AllowedCurrency, String> {

    private static final Set<String> ALLOWED = Set.of("USD", "EUR", "INR");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || ALLOWED.contains(value.toUpperCase());
    }
}
