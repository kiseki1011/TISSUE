package com.uranus.taskmanager.api.workspacemember.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleRequired {
	WorkspaceRole[] roles(); // 요구되는 권한을 배열로 받는다
}
