package com.tissue.api.issue.workflow.application;

import java.util.Map;

import org.springframework.lang.Nullable;

import com.tissue.api.issue.workflow.domain.gaurd.GuardType;

import jakarta.validation.constraints.NotNull;

public record GuardConfigData(
	@NotNull GuardType guardType,
	@Nullable Map<String, Object> params,
	@NotNull Integer order
) {
}
