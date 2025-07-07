package com.tissue.support.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tissue.support.mock.MockAuthorizationInterceptor;

@TestConfiguration
public class WebMvcTestConfig implements WebMvcConfigurer {

	@Value("${test.member.id:1}")
	private Long memberId;

	@Value("${test.allow.hasSufficientRole:true}")
	private boolean hasSufficientRole;

	@Value("${test.allow.isLogin:true}")
	private boolean isLogin;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MockAuthorizationInterceptor(hasSufficientRole))
			.order(1);
	}
}
