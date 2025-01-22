package com.tissue.api.common.validator.annotation.pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Pattern validation for a simple password
 * - Password must contain at least one letter of the alphabet and one number
 * - Special characters are optional
 * - Whitespaces are not allowed
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
	regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$)[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>]*$",
	message = "{valid.pattern.simple.password}"
)
public @interface SimplePasswordPattern {
	String message() default "{valid.pattern.simple.password}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
