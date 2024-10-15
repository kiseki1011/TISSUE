package com.uranus.taskmanager.api.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.auth.exception.UserNotLoggedInException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod method) {
			log.info("[AuthenticationInterceptor Invoked]");
			LoginRequired loginRequired = method.getMethodAnnotation(LoginRequired.class);
			if (loginRequired != null) {
				HttpSession session = request.getSession(false);
				if (session == null || session.getAttribute(SessionKey.LOGIN_MEMBER) == null) {
					throw new UserNotLoggedInException();
				}
			}
		}
		return true;
	}
}
