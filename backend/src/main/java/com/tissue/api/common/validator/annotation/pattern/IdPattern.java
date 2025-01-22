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
	regexp = "^[a-z][a-z0-9]+$",
	message = "{valid.pattern.id}"
)
public @interface IdPattern {
	String message() default "{valid.pattern.id}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
