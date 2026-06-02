package com.tissue.shared.auth;

import com.tissue.feature.member.domain.SystemRole;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Requires the caller to have system role {@link SystemRole#SUPER_ADMIN}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SUPER_ADMIN')")
public @interface RequireSuperAdmin {}
