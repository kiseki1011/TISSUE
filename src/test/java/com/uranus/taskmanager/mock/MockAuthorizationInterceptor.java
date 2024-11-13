package com.uranus.taskmanager.mock;

import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.workspacemember.authorization.exception.InsufficientWorkspaceRoleException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MockAuthorizationInterceptor implements HandlerInterceptor {
	private final boolean hasSufficientRole;

	public MockAuthorizationInterceptor(boolean hasSufficientRole) {
		this.hasSufficientRole = hasSufficientRole;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (hasSufficientRoleIsFalse()) {
			throw new InsufficientWorkspaceRoleException();
		}
		return true;
	}

	private boolean hasSufficientRoleIsFalse() {
		return !hasSufficientRole;
	}
}