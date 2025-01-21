package com.tissue.api.common.validator.annotation.pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Pattern validation for nicknames
 *  - Nickname must start with a letter and can only contain letters and numbers (e.g., John123, 길동이99)
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = "^[\\p{L}][\\p{L}\\p{N}]*$")
public @interface NicknamePattern {
	String message() default "{valid.pattern.nickname}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
