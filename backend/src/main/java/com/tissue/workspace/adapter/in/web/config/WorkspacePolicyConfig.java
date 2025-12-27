package com.tissue.workspace.adapter.in.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.workspace.domain.policy.WorkspacePolicy;

import jakarta.validation.constraints.Min;

@Configuration
public class WorkspacePolicyConfig {

	@Bean
	public WorkspacePolicy workspacePolicy(
		@Value("${workspace.policy.max-members:1000}") @Min(1) int maxMembers
	) {
		return new WorkspacePolicy(maxMembers);
	}
}
