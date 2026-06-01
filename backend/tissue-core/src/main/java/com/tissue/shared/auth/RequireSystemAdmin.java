package com.tissue.shared.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Requires the caller to have system role {@code ADMIN} or higher (i.e. {@code ADMIN} or
 * {@code SUPER_ADMIN}). Used for globally-managed resources (workflows, issue types, issue fields)
 * whose authorization is based purely on the actor's system role, not on project membership.
 *
 * <p>Enforced by Spring method security ({@code @EnableMethodSecurity}) via the inherited
 * {@link PreAuthorize} expression, matched against the {@code ROLE_*} authority carried on the JWT.
 * A {@code RoleHierarchy} bean maps {@code ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_USER}, so
 * {@code SUPER_ADMIN} also satisfies {@code hasRole('ADMIN')}.
 *
 * <p>NOTE: the authority is baked into the access token at login time, so a system-role change is
 * not reflected until the token is refreshed. This staleness is acceptable for these context-free
 * global gates.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface RequireSystemAdmin {}
