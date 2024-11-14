package com.uranus.taskmanager.api.global.resolver;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.uranus.taskmanager.api.security.authentication.constant.SessionKey;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

	private final MemberRepository memberRepository;

	/**
	 * 컨트롤러 메서드의 파라미터가 LoginMember 타입이 아니거나
	 * 애노테이션 @ResolveLoginMember가 붙지 않았으면 false를 리턴한다.
	 *
	 * @param parameter - 타입을 확인할 메서드 파라미터
	 * @return boolean
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {

		log.info("[LoginMemberArgumentResolver] supportsParameter() called");

		return isLoginMember(parameter)
			&& hasResolveLoginMemberAnnotation(parameter);
	}

	/**
	 * 세션의 존재 여부와 로그인 정보의 유효성을 검증한다.
	 * 유효한 세션과 로그인 정보인 경우 LoginMember로 변환해서 리턴한다.
	 *
	 * @param parameter - LoginMember를 받을 파라미터.
	 *                  이 파라미터는 {@link #supportsParameter}으로 넘겨져서 {@code true}를 반환해야 한다.
	 * @return LoginMemberDto
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

		log.info("[LoginMemberArgumentResolver] resolveArgument() called");

		HttpSession session = getSession(webRequest);
		Member member = getLoggedInMember(session);

		return LoginMember.from(member);
	}

	private HttpSession getSession(NativeWebRequest webRequest) {
		HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();
		HttpSession session = request.getSession(false);

		return Optional.ofNullable(session)
			.orElseThrow(UserNotLoggedInException::new);
	}

	private Member getLoggedInMember(HttpSession session) {
		return Optional.ofNullable((Long)session.getAttribute(SessionKey.LOGIN_MEMBER_ID))
			.map(id -> memberRepository.findById(id)
				.orElseThrow(MemberNotFoundException::new))
			.orElseThrow(UserNotLoggedInException::new);
	}

	private boolean hasResolveLoginMemberAnnotation(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ResolveLoginMember.class);
	}

	private boolean isLoginMember(MethodParameter parameter) {
		return parameter.getParameterType().equals(LoginMember.class);
	}
}
