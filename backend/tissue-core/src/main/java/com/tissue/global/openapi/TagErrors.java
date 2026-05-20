package com.tissue.global.openapi;

import com.tissue.feature.tag.domain.exception.TagErrorCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link TagErrorCode} values a controller method may produce.
 *
 * <p>Picked up by the OpenAPI customizer in {@code OpenApiConfig}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TagErrors {

    TagErrorCode[] value();
}
