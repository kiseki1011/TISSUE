package com.tissue.api.common.validator.annotation.pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Pattern validation for a password
 * - Password must contatin at least one letter of the alphabet, one number and one special character
 * - Whitespaces are not allowed
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
	regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=\\S+$).*$",
	message = "{valid.pattern.password}"
)
public @interface PasswordPattern {
	String message() default "{valid.pattern.password}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
