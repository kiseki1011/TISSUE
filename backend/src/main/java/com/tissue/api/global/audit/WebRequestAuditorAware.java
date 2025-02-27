package com.tissue.api.global.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authorization.interceptor.AuthorizationInterceptor;
import com.tissue.api.security.session.SessionAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebRequestAuditorAware implements AuditorAware<Long> {

	private static final String WORKSPACE_API_PREFIX = "/api/v1/workspaces";

	private final HttpSession session;
	private final HttpServletRequest request;

	@Override
	public Optional<Long> getCurrentAuditor() {

		String requestUri = request.getRequestURI();

		// workspace API인 경우 workspaceMemberId 반환, workspaceMemberId가 null이면 넘어가기
		if (requestUri.startsWith(WORKSPACE_API_PREFIX)) {
			Long workspaceMemberId = (Long)request.getAttribute(
				AuthorizationInterceptor.CURRENT_WORKSPACE_MEMBER_ID
			);

			if (workspaceMemberId != null) {
				return Optional.of(workspaceMemberId);
			}
		}

		// 그 외의 경우 memberId 반환
		return Optional.ofNullable(
			(Long)session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)
		);
	}
}
