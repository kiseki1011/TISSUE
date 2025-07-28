package com.tissue.api.issue.presentation.controller.dto.request;

public record TransitionRequest(
	String label,
	boolean isMainFlow,
	String sourceTempKey,  // refers to tempKey of step
	String targetTempKey
) {
}
