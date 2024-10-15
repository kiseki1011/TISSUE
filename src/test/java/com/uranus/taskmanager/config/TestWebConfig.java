package com.uranus.taskmanager.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.uranus.taskmanager.api.auth.AuthenticationInterceptor;
import com.uranus.taskmanager.api.auth.LoginMemberArgumentResolver;
import com.uranus.taskmanager.api.workspacemember.authorization.AuthorizationInterceptor;

import lombok.RequiredArgsConstructor;

@Profile("TestWebConfig")
@Configuration
@RequiredArgsConstructor
public class TestWebConfig implements WebMvcConfigurer {
	private final AuthorizationInterceptor authorizationInterceptor;
	private final AuthenticationInterceptor authenticationInterceptor;
	private final LoginMemberArgumentResolver loginMemberArgumentResolver;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authenticationInterceptor);
		registry.addInterceptor(authorizationInterceptor);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(loginMemberArgumentResolver);
	}
}
