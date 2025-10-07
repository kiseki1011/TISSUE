package com.tissue.api.common.validator.annotation.size;

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
	min = 4, max = 24,
	message = "{valid.size.id}"
)
public @interface IdSize {
	String message() default "{valid.size.id}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
