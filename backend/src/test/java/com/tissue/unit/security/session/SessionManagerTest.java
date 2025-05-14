package com.tissue.unit.security.session;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tissue.api.common.enums.PermissionType;
import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.api.security.session.SessionAttributes;
import com.tissue.api.security.session.SessionManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private HttpSession session;
	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private SessionManager sessionManager;

	@Test
	@DisplayName("로그인 응답 정보가 주어지면 세션에 해당 정보가 저장된다")
	void createLoginSession() {
		// given
		LoginResponse loginResponse = LoginResponse.builder()
			.memberId(1L)
			.loginId("tester")
			.email("test@test.com")
			.build();

		// when
		sessionManager.createLoginSession(session, loginResponse);

		// then
		verify(session).setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);
		verify(session).setAttribute(SessionAttributes.LOGIN_MEMBER_LOGIN_ID, "tester");
		verify(session).setAttribute(SessionAttributes.LOGIN_MEMBER_EMAIL, "test@test.com");
	}

	@Test
	@DisplayName("세션에 로그인 ID가 있으면 해당 ID가 반환된다")
	void getLoginMemberId_Success() {
		// given
		when(session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).thenReturn(1L);

		// when
		Optional<Long> result = sessionManager.getOptionalLoginMemberId(session);

		// then
		assertThat(result).isPresent()
			.contains(1L);
	}

	@Test
	@DisplayName("세션이 null이면 빈 Optional이 반환된다")
	void getLoginMemberId_WhenSessionIsNull() {
		// when
		Optional<Long> result = sessionManager.getOptionalLoginMemberId(null);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("세션에 유효한 회원 ID가 있으면 해당 회원 정보가 반환된다")
	void getLoginMember_Success() {
		// given
		Member member = Member.builder()
			.loginId("test")
			.email("test@test.com")
			.password("password")
			.build();

		when(session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).thenReturn(1L);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		// when
		Member result = sessionManager.getLoginMember(session);

		// then
		assertThat(result).isNotNull()
			.extracting("loginId", "email")
			.containsExactly("test", "test@test.com");
	}

	@Test
	@DisplayName("세션에 회원 ID가 없으면 예외가 발생한다")
	void getLoginMember_WhenNoIdInSession() {
		// given
		when(session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).thenReturn(null);

		// when & then
		assertThatThrownBy(() -> sessionManager.getLoginMember(session))
			.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	@DisplayName("세션의 회원 ID로 회원을 찾을 수 없으면 예외가 발생한다")
	void getLoginMember_WhenMemberNotFound() {
		// given
		when(session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).thenReturn(1L);
		when(memberRepository.findById(1L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> sessionManager.getLoginMember(session))
			.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	@DisplayName("업데이트 권한 생성 요청을 하면 세션에 권한 정보가 저장된다")
	void createUpdatePermission() {
		// when
		sessionManager.setTemporaryPermission(session, PermissionType.MEMBER_UPDATE);

		// then
		verify(session).setAttribute(SessionAttributes.PERMISSION_TYPE, PermissionType.MEMBER_UPDATE);
		verify(session).setAttribute(eq(SessionAttributes.PERMISSION_EXPIRES_AT), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("세션 무효화 요청이 오면 세션이 무효화된다")
	void invalidateSession() {
		// given
		when(request.getSession(false)).thenReturn(session);

		// when
		sessionManager.invalidateSession(request);

		// then
		verify(session).invalidate();
	}
}
