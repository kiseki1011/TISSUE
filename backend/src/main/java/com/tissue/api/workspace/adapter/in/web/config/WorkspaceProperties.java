package com.tissue.api.workspace.adapter.in.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "tissue.workspace")
public record WorkspaceProperties(
	@Min(1) int maxMemberCount
) {
}
