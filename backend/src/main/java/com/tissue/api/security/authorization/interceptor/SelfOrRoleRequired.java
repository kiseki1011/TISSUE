package com.tissue.api.security.authorization.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SelfOrRoleRequired {
	WorkspaceRole role(); // 허용 최소 Role

	String memberIdParam() default "memberId"; // 경로 변수 이름 (기본: "memberId")
}
