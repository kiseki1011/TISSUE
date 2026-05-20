package com.tissue.global.openapi;

import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link IssueErrorCode} values a controller method may produce.
 *
 * <p>Picked up by the OpenAPI customizer in {@code OpenApiConfig}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IssueErrors {

    IssueErrorCode[] value();
}
