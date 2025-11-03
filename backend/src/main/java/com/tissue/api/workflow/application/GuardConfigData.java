package com.tissue.api.workflow.application;

import java.util.Map;

import org.springframework.lang.Nullable;

import com.tissue.api.workflow.domain.gaurd.GuardType;

import jakarta.validation.constraints.NotNull;

public record GuardConfigData(
	@NotNull GuardType guardType,
	@Nullable Map<String, Object> params,
	@NotNull Integer order
) {
}
