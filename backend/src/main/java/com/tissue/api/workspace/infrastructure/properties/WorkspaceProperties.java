package com.tissue.api.workspace.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "tissue.workspace")
@Validated
public record WorkspaceProperties(
	@Min(1) int maxMemberCount
) {
}
