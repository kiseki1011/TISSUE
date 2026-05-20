package com.tissue.global.openapi;

import com.tissue.feature.sprint.domain.exception.SprintErrorCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link SprintErrorCode} values a controller method may produce.
 *
 * <p>Picked up by the OpenAPI customizer in {@code OpenApiConfig}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SprintErrors {

    SprintErrorCode[] value();
}
