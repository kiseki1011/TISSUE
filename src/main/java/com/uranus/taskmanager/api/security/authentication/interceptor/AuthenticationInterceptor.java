package com.uranus.taskmanager.api.security.authentication.interceptor;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.security.authentication.session.SessionValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
	private final SessionValidator sessionValidator;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isNotHandlerMethod(handler)) {
			return true;
		}

		log.info("[AuthenticationInterceptor Invoked]");

		HandlerMethod method = (HandlerMethod)handler;
		if (isLoginRequired(method)) {
			sessionValidator.validateLoginStatus(request);
		}

		return true;
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
