package com.uranus.taskmanager.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import com.uranus.taskmanager.api.authentication.AuthenticationInterceptor;
import com.uranus.taskmanager.api.authentication.LoginMemberArgumentResolver;
import com.uranus.taskmanager.api.global.config.WebMvcConfig;
import com.uranus.taskmanager.api.workspacemember.authorization.AuthorizationInterceptor;

@Profile("TestWebMvcConfig")
@TestConfiguration
public class TestWebMvcConfig extends WebMvcConfig {
	public TestWebMvcConfig(AuthorizationInterceptor authorizationInterceptor,
		AuthenticationInterceptor authenticationInterceptor, LoginMemberArgumentResolver loginMemberArgumentResolver) {
		super(authorizationInterceptor, authenticationInterceptor, loginMemberArgumentResolver);
	}
}
