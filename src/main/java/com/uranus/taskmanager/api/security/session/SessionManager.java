package com.uranus.taskmanager.api.security.session;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.response.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionManager {
	private final MemberRepository memberRepository;
	private static final int UPDATE_PERMISSION_MINUTES = 5;

	// 로그인 세션 설정, 관리
	public void createLoginSession(HttpSession session, LoginResponse loginResponse) {
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, loginResponse.getId());
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_LOGIN_ID, loginResponse.getLoginId());
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_EMAIL, loginResponse.getEmail());
		log.info("Login session created for member ID: {}", loginResponse.getId());
	}

	public Member getLoginMember(HttpSession session) {
		return getLoginMemberId(session)
			.map(id -> memberRepository.findById(id)
				.orElseThrow(MemberNotFoundException::new))
			.orElseThrow(UserNotLoggedInException::new);
	}

	public Optional<Long> getLoginMemberId(HttpSession session) {
		return Optional.ofNullable(session)
			.map(s -> (Long)s.getAttribute(SessionAttributes.LOGIN_MEMBER_ID));
	}

	// 업데이트 권한 설정, 관리
	public void createUpdatePermission(HttpSession session) {
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(UPDATE_PERMISSION_MINUTES);
		session.setAttribute(SessionAttributes.UPDATE_AUTH, true);
		session.setAttribute(SessionAttributes.UPDATE_AUTH_EXPIRES_AT, expiresAt);
		log.info("Update permission created, expires at: {}", expiresAt);
	}

	// 세션 정보 업데이트
	public void updateSessionEmail(HttpSession session, String newEmail) {
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_EMAIL, newEmail);
		log.info("Session email updated to: {}", newEmail);
	}

	// 세션 종료
	public void invalidateSession(HttpServletRequest request) {
		Optional.ofNullable(request.getSession(false))
			.ifPresent(session -> {
				session.invalidate();
				log.info("Session invalidated.");
			});
	}

	// HttpSession 조회
	public HttpSession getSession(NativeWebRequest webRequest) {
		HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();

		return Optional.ofNullable(request.getSession(false))
			.orElseThrow(UserNotLoggedInException::new);
	}
}
