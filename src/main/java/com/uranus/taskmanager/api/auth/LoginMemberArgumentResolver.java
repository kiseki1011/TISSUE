package com.uranus.taskmanager.api.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.auth.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

	private final MemberRepository memberRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		/*
		 * 컨트롤러 메서드의 파라미터가 LoginMemberDto 타입인지 확인
		 * 파라미터에 @LoginMember 애노테이션이 붙었는지 확인
		 */
		return parameter.getParameterType().equals(LoginMemberDto.class)
			&& parameter.hasParameterAnnotation(LoginMember.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		/*
		 * 세션을 가져와서 확인한다
		 * 세션이 없거나 세션에 LOGIN_MEMBER라는 키로 로그인된 사용자 정보가 없다면 UserNotLoggedInException
		 */
		HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(SessionKey.LOGIN_MEMBER) == null) {
			throw new UserNotLoggedInException();
		}

		/*
		 * 세션에서 로그인된 사용자의 정보를 가져온다
		 * loginId를 통해서 Member를 조회한다
		 */
		String loginId = (String)session.getAttribute(SessionKey.LOGIN_MEMBER);

		Member member = memberRepository.findByLoginId(loginId)
			.orElseThrow(MemberNotFoundException::new);

		/*
		 * member를 LoginMemberDto로 변환해서 반환한다
		 */
		return LoginMemberDto.fromEntity(member);
	}
}
