package com.tissue.api.security.session;

import static com.tissue.api.security.session.SessionAttributes.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import com.tissue.api.common.enums.PermissionType;
import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.common.exception.UnauthorizedException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionManager {
	private static final int UPDATE_PERMISSION_MINUTES = 3;
	private static final int DELETE_PERMISSION_MINUTES = 1;

	private final MemberRepository memberRepository;

	public void createLoginSession(HttpSession session, LoginResponse loginResponse) {
		session.setAttribute(LOGIN_MEMBER_ID, loginResponse.getMemberId());
		session.setAttribute(LOGIN_MEMBER_LOGIN_ID, loginResponse.getLoginId());
		session.setAttribute(LOGIN_MEMBER_EMAIL, loginResponse.getEmail());
		log.info("Login session created for MEMBER_ID: {}", loginResponse.getMemberId());
	}

	public Member getLoginMember(HttpSession session) {
		return getOptionalLoginMemberId(session)
			.map(id -> memberRepository.findById(id)
				.orElseThrow(() -> new MemberNotFoundException(id)))
			.orElseThrow(() -> new UnauthorizedException("Login is required to access."));
	}

	public Optional<Long> getOptionalLoginMemberId(HttpSession session) {
		return Optional.ofNullable(session)
			.map(s -> (Long)s.getAttribute(LOGIN_MEMBER_ID));
	}

	public void setTemporaryPermission(HttpSession session, PermissionType permissionType) {
		LocalDateTime expiresAt = LocalDateTime.now();

		expiresAt = switch (permissionType) {
			case MEMBER_UPDATE, WORKSPACE_PASSWORD_UPDATE -> expiresAt.plusMinutes(UPDATE_PERMISSION_MINUTES);
			case MEMBER_DELETE, WORKSPACE_DELETE -> expiresAt.plusMinutes(DELETE_PERMISSION_MINUTES);
			default -> throw new InvalidOperationException("Permission type does not exist.");
		};

		session.setAttribute(SessionAttributes.PERMISSION_TYPE, permissionType);
		session.setAttribute(SessionAttributes.PERMISSION_EXISTS, true);
		session.setAttribute(SessionAttributes.PERMISSION_EXPIRES_AT, expiresAt);

		log.info("{} permission granted. Expires at: {}", permissionType, expiresAt);
	}

	public void clearPermission(HttpSession session) {
		session.removeAttribute(SessionAttributes.PERMISSION_TYPE);
		session.removeAttribute(SessionAttributes.PERMISSION_EXISTS);
		session.removeAttribute(SessionAttributes.PERMISSION_EXPIRES_AT);
		log.info("Permission cleared.");
	}

	public void updateSessionEmail(HttpSession session, String newEmail) {
		session.setAttribute(LOGIN_MEMBER_EMAIL, newEmail);
	}

	public void invalidateSession(HttpServletRequest request) {
		Optional.ofNullable(request.getSession(false))
			.ifPresent(session -> {
				session.invalidate();
				log.info("Session invalidated.");
			});
	}

	public HttpSession getSession(NativeWebRequest webRequest) {
		HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();

		return Optional.ofNullable(request.getSession(false))
			.orElseThrow(() -> new UnauthorizedException("Login is required to access."));
	}
}
