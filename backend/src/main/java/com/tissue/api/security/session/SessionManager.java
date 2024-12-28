package com.tissue.api.security.session;

import static com.tissue.api.security.session.SessionAttributes.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.security.authentication.exception.UserNotLoggedInException;
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
		log.info("Login session created for member ID: {}", loginResponse.getMemberId());
	}

	public Member getLoginMember(HttpSession session) {
		return getLoginMemberId(session)
			.map(id -> memberRepository.findById(id)
				.orElseThrow(MemberNotFoundException::new))
			.orElseThrow(UserNotLoggedInException::new);
	}

	public Optional<Long> getLoginMemberId(HttpSession session) {
		return Optional.ofNullable(session)
			.map(s -> (Long)s.getAttribute(LOGIN_MEMBER_ID));
	}

	public void setTemporaryUpdatePermission(HttpSession session) {
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(UPDATE_PERMISSION_MINUTES);
		session.setAttribute(MEMBER_UPDATE_AUTH, true);
		session.setAttribute(MEMBER_UPDATE_AUTH_EXPIRES_AT, expiresAt);
		log.info("Update permission created, expires at: {}", expiresAt);
	}

	public void setTemporaryDeletePermission(HttpSession session) {
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(DELETE_PERMISSION_MINUTES);
		session.setAttribute(MEMBER_DELETE_AUTH, true);
		session.setAttribute(MEMBER_DELETE_AUTH_EXPIRES_AT, expiresAt);
		log.info("Delete permission created, expires at: {}", expiresAt);
	}

	public void updateSessionEmail(HttpSession session, String newEmail) {
		session.setAttribute(LOGIN_MEMBER_EMAIL, newEmail);
		log.info("Session email updated to: {}", newEmail);
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
			.orElseThrow(UserNotLoggedInException::new);
	}
}
