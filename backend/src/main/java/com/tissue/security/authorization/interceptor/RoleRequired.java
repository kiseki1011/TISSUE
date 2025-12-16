package com.tissue.security.authorization.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tissue.workspace.domain.enums.WorkspaceRole;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleRequired {
	WorkspaceRole role(); // required minimum role
}
