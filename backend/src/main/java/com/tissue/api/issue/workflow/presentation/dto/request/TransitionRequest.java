package com.tissue.api.issue.workflow.presentation.dto.request;

public record TransitionRequest(
	String label,
	String description,
	boolean mainFlow,
	String sourceTempKey,  // refers to tempKey of status
	String targetTempKey
) {
}
