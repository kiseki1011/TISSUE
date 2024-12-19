package com.uranus.taskmanager.api.global.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.security.session.SessionAttributes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionAuditorAware implements AuditorAware<Long> {
	private final HttpSession session;

	@Override
	public Optional<Long> getCurrentAuditor() {
		Long loginMemberId = (Long)session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID);
		return Optional.ofNullable(loginMemberId);
	}
}
