package com.shopflow.order.web.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CUSTOM VALIDATION ANNOTATION - the two-part recipe:
 *   1. this annotation, marked @Constraint(validatedBy = <validator class>)
 *   2. the validator implementing ConstraintValidator (does the actual check)
 * After that it composes with @Valid exactly like the built-in constraints.
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
