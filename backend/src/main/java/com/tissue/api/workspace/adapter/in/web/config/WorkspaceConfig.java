package com.tissue.api.workspace.adapter.in.web.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.application.service.command.WorkspaceCreateService;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspace.domain.port.out.WorkspaceCommandRepository;

@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfig {

	@Bean
	public WorkspacePolicy workspacePolicy(WorkspaceProperties props) {
		return new WorkspacePolicy(props.maxMemberCount());
	}

	public WorkspaceCreateUseCase workspaceCreateService(
		MemberFinder memberFinder,
		WorkspaceCommandRepository workspaceCommandRepository
	) {
		return new WorkspaceCreateService(
			memberFinder,
			workspaceCommandRepository
		);
	}
}
