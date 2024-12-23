package com.tissue.mock;

import org.springframework.web.servlet.HandlerInterceptor;

import com.tissue.api.security.authentication.exception.UserNotLoggedInException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MockAuthenticationInterceptor implements HandlerInterceptor {
	private final boolean isLogin;

	public MockAuthenticationInterceptor(boolean isLogin) {
		this.isLogin = isLogin;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isLoginIsFalse()) {
			throw new UserNotLoggedInException();
		}
		return true;
	}

	private boolean isLoginIsFalse() {
		return !isLogin;
	}
}
