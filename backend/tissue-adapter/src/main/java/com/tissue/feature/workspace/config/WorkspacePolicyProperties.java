package com.tissue.feature.workspace.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tissue.workspace.policy")
public record WorkspacePolicyProperties(
        @Min(1) Integer maxMembers, @Min(1) Integer maxProjects) {

    public WorkspacePolicyProperties {
        if (maxMembers == null) maxMembers = 1000;
        if (maxProjects == null) maxProjects = 100;
    }
}
