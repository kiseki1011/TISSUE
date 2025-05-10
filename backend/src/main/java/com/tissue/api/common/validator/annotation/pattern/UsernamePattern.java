package com.tissue.api.common.validator.annotation.pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
	regexp = "^\\p{L}[\\p{L}\\p{N}]*$",
	message = "{valid.pattern.username}"
)
public @interface UsernamePattern {
	String message() default "{valid.pattern.username}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
