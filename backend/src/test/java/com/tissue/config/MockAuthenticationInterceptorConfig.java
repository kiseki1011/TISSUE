package com.tissue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tissue.mock.MockAuthenticationInterceptor;

@TestConfiguration
public class MockAuthenticationInterceptorConfig implements WebMvcConfigurer {
	@Value("${test.allow.isLogin:true}")
	private boolean isLogin;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MockAuthenticationInterceptor(isLogin));
	}
}
