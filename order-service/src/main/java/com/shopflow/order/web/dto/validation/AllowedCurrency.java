package com.shopflow.order.web.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks that a currency code is one this shop accepts.
 * The check itself lives in AllowedCurrencyValidator.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedCurrencyValidator.class)
public @interface AllowedCurrency {

    String message() default "currency must be one of USD, EUR, INR";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
