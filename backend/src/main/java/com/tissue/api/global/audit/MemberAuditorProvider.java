package com.tissue.api.global.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.tissue.api.security.session.SessionAttributes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberAuditorProvider implements AuditorAware<Long> {

	private final HttpSession session;

	@Override
	public Optional<Long> getCurrentAuditor() {

		return Optional.ofNullable(
			(Long)session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)
		);
	}
}
