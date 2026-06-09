package com.tissue.shared.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint (or controller) that is only usable when the instance runs in OIDC
 * authentication mode ({@code tissue.auth.mode=OIDC}).
 *
 * <p>The identity provider (IdP) owns authentication and should be the source of truth for email/name.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OidcAuthOnly {}
