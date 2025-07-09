package com.tissue.api.security.authorization.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SelfOrRoleRequired {
	WorkspaceRole role(); // required minimum role

	String memberIdParam() default "memberId"; // path variable name
}
