package com.tissue.api.workspace.adapter.in.web.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.workspace.application.service.WorkspaceCreateService;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspace.adapter.in.web.config.WorkspaceProperties;
import com.tissue.api.workspace.domain.port.out.WorkspaceRepository;

@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfig {

	@Bean
	public WorkspacePolicy workspacePolicy(WorkspaceProperties props) {
		return new WorkspacePolicy(props.maxMemberCount());
	}

	public WorkspaceCreateUseCase workspaceCreateService(
		MemberFinder memberFinder,
		WorkspaceRepository workspaceRepository,
		PasswordEncoder passwordEncoder,
		WorkspacePolicy workspacePolicy
	) {
		return new WorkspaceCreateService(
			memberFinder,
			workspaceRepository,
			passwordEncoder,
			workspacePolicy
		);
	}
}
