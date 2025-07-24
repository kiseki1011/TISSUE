package com.tissue.api.issue.presentation.controller.dto.request;

public record TransitionRequest(
	String label, boolean isMainFlow, int sourceStepIndex, int targetStepIndex
) {
}
