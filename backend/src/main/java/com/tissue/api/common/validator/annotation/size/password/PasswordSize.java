package com.tissue.api.common.validator.annotation.size.password;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(
	min = 8, max = 50,
	message = "{valid.size.password}"
)
public @interface PasswordSize {
	String message() default "{valid.size.password}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
