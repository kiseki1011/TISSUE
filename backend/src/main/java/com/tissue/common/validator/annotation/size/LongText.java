package com.tissue.common.validator.annotation.size;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(max = 2000, message = "{valid.size.long}")
public @interface LongText {
    String message() default "{valid.size.long}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
