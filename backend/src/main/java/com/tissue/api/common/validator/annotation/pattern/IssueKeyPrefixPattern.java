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
	regexp = "^[a-zA-Z]+$",
	message = "{valid.pattern.issuekeyprefix}"
)
public @interface IssueKeyPrefixPattern {
	String message() default "{valid.pattern.issuekeyprefix}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
