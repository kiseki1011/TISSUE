package com.tissue.api.issue.workflow.presentation.dto.request;

public record StatusRequest(
	String tempKey, // Set the tempKey at the client. UUID recommended
	String label,
	String description,
	boolean initial,
	boolean terminal
) {
}
