package com.uranus.taskmanager.api.global.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.security.session.SessionAttributes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionAuditorAware implements AuditorAware<String> {
	private final HttpSession session;

	@Override
	public Optional<String> getCurrentAuditor() {
		String loginId = (String)session.getAttribute(SessionAttributes.LOGIN_MEMBER_LOGIN_ID);
		return Optional.ofNullable(loginId);
	}
}
