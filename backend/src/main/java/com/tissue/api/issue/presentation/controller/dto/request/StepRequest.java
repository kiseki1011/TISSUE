package com.tissue.api.issue.presentation.controller.dto.request;

public record StepRequest(
	String tempKey, // Set the tempKey like "step_1", "step_2" at the client
	String label,
	boolean isInitial,
	boolean isFinal
) {
}
