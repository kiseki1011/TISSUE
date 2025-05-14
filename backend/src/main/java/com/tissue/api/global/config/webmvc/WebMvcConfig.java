package com.tissue.api.global.config.webmvc;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tissue.api.security.authentication.interceptor.AuthenticationInterceptor;
import com.tissue.api.security.authentication.resolver.LoginMemberArgumentResolver;
import com.tissue.api.security.authorization.interceptor.RoleRequiredInterceptor;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequiredInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final RoleRequiredInterceptor roleRequiredInterceptor;
	private final SelfOrRoleRequiredInterceptor selfOrRoleRequiredInterceptor;
	private final AuthenticationInterceptor authenticationInterceptor;
	private final LoginMemberArgumentResolver loginMemberArgumentResolver;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authenticationInterceptor)
			.order(1);
		registry.addInterceptor(roleRequiredInterceptor)
			.order(2);
		registry.addInterceptor(selfOrRoleRequiredInterceptor)
			.order(3);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(loginMemberArgumentResolver);
	}
}
