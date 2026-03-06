package com.tissue.feature.workspace.config;

import com.tissue.feature.workspace.domain.policy.WorkspacePolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkspacePolicyProperties.class)
public class WorkspacePolicyConfig {

    @Bean
    public WorkspacePolicy workspacePolicy(WorkspacePolicyProperties properties) {
        return new WorkspacePolicy(properties.maxMembers(), properties.maxProjects());
    }
}
