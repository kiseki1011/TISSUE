package com.tissue.api.common.validator.annotation.pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Pattern validation for names
 *  - Name must contain only letters (such as a-z, A-Z, 가-힣) without numbers or special characters
 *  - A single space between letters is allowed
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
	regexp = "^$|^\\p{L}+( \\p{L}+)*$",
	message = "{valid.pattern.name}"
)
public @interface NamePattern {
	String message() default "{valid.pattern.name}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
