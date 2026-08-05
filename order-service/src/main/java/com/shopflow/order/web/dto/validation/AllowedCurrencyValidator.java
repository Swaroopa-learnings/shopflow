package com.shopflow.order.web.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/** The logic behind @AllowedCurrency. Null passes: pair with @NotNull to make the field required. */
public class AllowedCurrencyValidator implements ConstraintValidator<AllowedCurrency, String> {

    private static final Set<String> ALLOWED = Set.of("USD", "EUR", "INR");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || ALLOWED.contains(value.toUpperCase());
    }
}
