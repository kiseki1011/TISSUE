package com.tissue.api.issue.workflow.presentation.dto.request;

public record StatusRequest(
	String tempKey, // Set the tempKey like "status_1", "status_2" at the client
	String label,
	String description,
	boolean isInitial,
	boolean isFinal
) {
}
