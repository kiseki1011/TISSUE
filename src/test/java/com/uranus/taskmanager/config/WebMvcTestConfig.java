package com.uranus.taskmanager.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.uranus.taskmanager.api.authentication.dto.LoginMember;
import com.uranus.taskmanager.mock.MockAuthenticationInterceptor;
import com.uranus.taskmanager.mock.MockAuthorizationInterceptor;
import com.uranus.taskmanager.mock.MockLoginMemberArgumentResolver;

@TestConfiguration
public class WebMvcTestConfig implements WebMvcConfigurer {
	@Value("${test.member.id:1}")
	private Long id;

	@Value("${test.member.loginId:member1}")
	private String loginId;

	@Value("${test.member.email.member1@test.com}")
	private String email;
	@Value("${test.allow.hasSufficientRole:true}")
	private boolean hasSufficientRole;
	@Value("${test.allow.isLogin:true}")
	private boolean isLogin;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		LoginMember loginMember = LoginMember.builder()
			.id(id)
			.loginId(loginId)
			.email(email)
			.build();
		resolvers.add(new MockLoginMemberArgumentResolver(loginMember));
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MockAuthorizationInterceptor(hasSufficientRole));
		registry.addInterceptor(new MockAuthenticationInterceptor(isLogin));
	}
}
