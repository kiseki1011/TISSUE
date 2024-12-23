package com.tissue.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tissue.mock.MockAuthorizationInterceptor;
import com.tissue.mock.MockLoginMemberArgumentResolver;
import com.tissue.mock.MockAuthenticationInterceptor;

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
		resolvers.add(new MockLoginMemberArgumentResolver(id));
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MockAuthorizationInterceptor(hasSufficientRole));
		registry.addInterceptor(new MockAuthenticationInterceptor(isLogin));
	}
}
