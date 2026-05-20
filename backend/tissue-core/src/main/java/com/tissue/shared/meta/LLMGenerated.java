package com.tissue.shared.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks code that was generated or assisted by an LLM.
 *
 * <p>Source-only annotation. Serves as documentation for tracking
 * LLM generated code.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface LLMGenerated {

    LLMInvolvement llmInvolvement();

    Evaluation evaluation() default Evaluation.NOT_REVIEWED;

    String evaluationReason() default "";

    String generatedAt() default "";

    String model() default "";

    String agentName() default "";

    String reviewedBy() default "";

    String reviewedAt() default "";
}
