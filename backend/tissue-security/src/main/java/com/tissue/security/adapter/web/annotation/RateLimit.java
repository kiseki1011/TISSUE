package com.tissue.security.adapter.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String prefix();

    /**
     * SpEL expression to extract the rate limit key.
     *
     * <p>Example: {@code "#request.email()"}</p>
     */
    String key();

    int maxRequests();

    int window();

    TimeUnit timeUnit();
}
