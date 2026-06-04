package com.tissue.shared.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks endpoint (or controller) that is used when the instance uses LOCAL authentication
 * (Tissue's own username/email + password).
 *
 * <p>When {@code tissue.auth.mode=OIDC} the identity provider owns
 * authentication and should be the source of truth for email/name.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalAuthOnly {}
