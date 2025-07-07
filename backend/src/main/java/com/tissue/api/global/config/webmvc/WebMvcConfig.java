package com.tissue.api.global.config.webmvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tissue.api.security.authorization.interceptor.RoleRequiredInterceptor;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequiredInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final RoleRequiredInterceptor roleRequiredInterceptor;
	private final SelfOrRoleRequiredInterceptor selfOrRoleRequiredInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(roleRequiredInterceptor)
			.order(1);
		registry.addInterceptor(selfOrRoleRequiredInterceptor)
			.order(2);
	}
}
