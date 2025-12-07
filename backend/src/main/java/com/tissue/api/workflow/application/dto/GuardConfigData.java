package com.tissue.api.workflow.application.dto;

import java.util.Map;

import org.springframework.lang.Nullable;

import com.tissue.api.workflow.domain.guard.GuardType;

import jakarta.validation.constraints.NotNull;

public record GuardConfigData(
	@NotNull GuardType guardType,
	@Nullable Map<String, Object> params,
	@NotNull Integer order
) {
}
