package com.tissue.api.issue.presentation.controller.dto.request;

public record StepRequest(
	String label, boolean isInitial, boolean isFinal
) {
}
