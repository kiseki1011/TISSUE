package com.uranus.taskmanager.api.authentication;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.authentication.exception.UserNotLoggedInException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isNotHandlerMethod(handler)) {
			return true;
		}

		log.info("[AuthenticationInterceptor Invoked]");

		HandlerMethod method = (HandlerMethod)handler;
		if (isLoginRequired(method)) {
			validateUserSession(request);
		}

		return true;
	}

	private static void validateUserSession(HttpServletRequest request) {
		Optional<HttpSession> session = Optional.ofNullable(request.getSession(false));

		if (session.isEmpty() || session.map(s -> s.getAttribute(SessionKey.LOGIN_MEMBER)).isEmpty()) {
			throw new UserNotLoggedInException();
		}
	}

	private boolean isNotHandlerMethod(Object handler) {
		return !(handler instanceof HandlerMethod);
	}

	private boolean isLoginRequired(HandlerMethod method) {
		Optional<LoginRequired> optionalAnnotation = Optional.ofNullable(
			method.getMethodAnnotation(LoginRequired.class));
		return optionalAnnotation.isPresent();
	}

}
