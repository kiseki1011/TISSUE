package com.uranus.taskmanager.mock;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MockAuthorizationInterceptor implements HandlerInterceptor {
	private final boolean allowAccess;

	public MockAuthorizationInterceptor(boolean allowAccess) {
		this.allowAccess = allowAccess;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		return allowAccess;
	}
}
