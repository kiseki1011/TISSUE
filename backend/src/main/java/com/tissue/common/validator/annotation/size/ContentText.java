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
@Size(max = 65535, message = "{valid.size.content}")
public @interface ContentText {
    String message() default "{valid.size.content}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
