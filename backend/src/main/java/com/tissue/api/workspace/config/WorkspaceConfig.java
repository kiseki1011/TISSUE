package com.tissue.api.workspace.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateRetryOnCodeCollisionService;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspace.infrastructure.properties.WorkspaceProperties;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfig {

	@Bean
	public WorkspacePolicy workspacePolicy(WorkspaceProperties props) {
		return new WorkspacePolicy(props.maxMemberCount());
	}

	public WorkspaceCreateService workspaceCreateService(
		MemberFinder memberFinder,
		WorkspaceRepository workspaceRepository,
		WorkspaceMemberRepository workspaceMemberRepository,
		PasswordEncoder passwordEncoder,
		WorkspacePolicy workspacePolicy
	) {
		return new WorkspaceCreateRetryOnCodeCollisionService(
			memberFinder,
			workspaceRepository,
			workspaceMemberRepository,
			passwordEncoder,
			workspacePolicy
		);
	}
}
