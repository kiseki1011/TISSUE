package deprecated.com.tissue.support.mock;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;

import deprecated.com.tissue.support.fixture.MemberBuilderForTest;

public class MockCurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentMember.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Member mockMember = new MemberBuilderForTest().build();

		MemberUserDetails mockMemberUserDetails = new MemberUserDetails(mockMember);
		mockMemberUserDetails.setElevated(true);

		return mockMemberUserDetails;
	}
}
