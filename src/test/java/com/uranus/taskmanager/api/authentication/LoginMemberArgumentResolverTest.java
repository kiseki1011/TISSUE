package com.uranus.taskmanager.api.authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class LoginMemberArgumentResolverTest {
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private NativeWebRequest webRequest;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpSession session;

	@InjectMocks
	private LoginMemberArgumentResolver resolver;

	private MemberEntityFixture memberEntityFixture;

	@BeforeEach
	public void setUp() {
		memberEntityFixture = new MemberEntityFixture();
	}

	// LoginMemberDto 타입을 사용하는 더미 메서드
	public void dummyMethod(@LoginMember LoginMemberDto loginMemberDto) {
	}

	// String 타입을 사용하는 더미 메서드
	public void dummyMethod(@LoginMember String loginMemberDto) {
	}

	// @LoginMember 애노테이션을 사용하지 않는 더미 메서드
	public void dummyMethodWithoutAnnotation(LoginMemberDto loginMemberDto) {
	}

	@Test
	@DisplayName("supportsParameter는 메서드가 @LoginMember 애노테이션을 사용하고, 파라미터가 LoginMemberDto 때 true를 반환한다")
	void test1() throws Exception {

		// given
		// LoginMemberDto 파라미터를 가진 dummyMethod의 MethodParameter 생성
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethod", LoginMemberDto.class), 0);

		// when
		// supportsParameter 호출하여 LoginMemberDto 타입과 @LoginMember 애노테이션 확인
		boolean result = resolver.supportsParameter(parameter);

		// then
		// LoginMemberDto 타입이며 @LoginMember 애노테이션이 있을 때 true 반환 검증
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("supportsParameter는 메서드 파라미터가 LoginMemberDto가 아니면 false를 반환한다")
	void test2() throws Exception {

		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethod", String.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("supportsParameter는 메서드가 @LoginMember 애노테이션을 사용하지 않는 경우 false를 반환한다")
	void test3() throws Exception {

		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethodWithoutAnnotation", LoginMemberDto.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("resolveArgument는 로그인된 회원을 DTO로 반환한다")
	void test4() throws Exception {

		// given
		// 가상의 로그인 ID 설정
		String loginId = "user123";
		String email = "user123@test.com";
		Member member = memberEntityFixture.createMember(loginId, email);

		// NativeWebRequest에서 HttpServletRequest를 가져오는 부분 모킹
		when((HttpServletRequest)webRequest.getNativeRequest()).thenReturn(request);

		// HttpServletRequest에서 세션을 가져오는 부분 모킹
		when(request.getSession(false)).thenReturn(session);

		// 세션에서 로그인된 사용자의 ID를 가져오는 부분 모킹
		when(session.getAttribute(SessionKey.LOGIN_MEMBER)).thenReturn(loginId);

		// MemberRepository에서 해당 로그인 ID로 사용자를 조회하는 부분 모킹
		when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));

		// when
		// resolveArgument 호출
		LoginMemberDto result = (LoginMemberDto)resolver.resolveArgument(null, null, webRequest, null);

		// then
		// 반환된 LoginMemberDto가 null이 아니며, 로그인 ID가 일치하는지 확인
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(LoginMemberDto.class);
		assertThat(result.getLoginId()).isEqualTo(loginId);
	}

	@Test
	@DisplayName("resolveArgument는 세션이 없으면 UserNotLoggedInException를 던진다")
	void test5() {
		// given
		when((HttpServletRequest)webRequest.getNativeRequest()).thenReturn(request);
		when(request.getSession(false)).thenReturn(null);

		// when & then
		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
			.isInstanceOf(UserNotLoggedInException.class);
	}

	@Test
	@DisplayName("resolveArgument는 세션에 로그인 정보가 없으면 UserNotLoggedInException을 던진다")
	void test6() {
		// given
		when((HttpServletRequest)webRequest.getNativeRequest()).thenReturn(request);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(SessionKey.LOGIN_MEMBER)).thenReturn(null);

		// when & then
		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
			.isInstanceOf(UserNotLoggedInException.class);
	}

	@Test
	@DisplayName("resolveArgument는 세션의 정보를 사용해 회원을 못 찾으면 MemberNotFoundException을 던진다")
	void test7() {
		// given
		String loginId = "invalidUser";

		when((HttpServletRequest)webRequest.getNativeRequest()).thenReturn(request);
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(SessionKey.LOGIN_MEMBER)).thenReturn(loginId);
		when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
			.isInstanceOf(MemberNotFoundException.class);
	}

}