package com.uranus.taskmanager.api.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod method) {

			// @LoginRequired 애노테이션이 있는지 확인
			LoginRequired loginRequired = method.getMethodAnnotation(LoginRequired.class);
			if (loginRequired != null) {
				HttpSession session = request.getSession(false); // 세션에서 로그인 정보 확인
				if (session == null || session.getAttribute(SessionKey.LOGIN_MEMBER) == null) {
					throw new RuntimeException("Unauthorized"); // Todo: UnauthorizedException 만들기
				}
			}
		}
		return true;
	}
}
