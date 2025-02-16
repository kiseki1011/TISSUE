package com.tissue.api.global.config.webmvc;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tissue.api.security.authentication.interceptor.AuthenticationInterceptor;
import com.tissue.api.security.authentication.resolver.LoginMemberArgumentResolver;
import com.tissue.api.security.authorization.interceptor.AuthorizationInterceptor;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMemberArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final AuthorizationInterceptor authorizationInterceptor;
	private final AuthenticationInterceptor authenticationInterceptor;
	private final LoginMemberArgumentResolver loginMemberArgumentResolver;
	private final CurrentWorkspaceMemberArgumentResolver currentWorkspaceMemberArgumentResolver;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authenticationInterceptor)
			.order(1);
		registry.addInterceptor(authorizationInterceptor)
			.order(2);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(loginMemberArgumentResolver);
		resolvers.add(currentWorkspaceMemberArgumentResolver);
	}
}
