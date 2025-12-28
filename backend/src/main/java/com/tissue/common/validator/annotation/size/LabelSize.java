package com.tissue.common.validator.annotation.size;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(max = 32, message = "{valid.size.label}")
public @interface LabelSize {
    String message() default "{valid.size.label}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
