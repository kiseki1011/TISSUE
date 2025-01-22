package com.tissue.api.common.validator.annotation.size.text;

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
	max = 50,
	message = "{valid.size.short}"
)
public @interface ShortText {
	String message() default "{valid.size.short}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
