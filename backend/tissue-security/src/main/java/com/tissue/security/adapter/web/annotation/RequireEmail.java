package com.tissue.security.adapter.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint that requires email functionality to be enabled.
 * When {@code tissue.security.email-required} is {@code false},
 * annotated endpoints will return 400 BAD_REQUEST.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireEmail {}
