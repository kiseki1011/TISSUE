package com.tissue.api.issue.base.presentation.dto.request;

import java.util.List;

import com.tissue.api.issue.base.domain.enums.FieldType;

public record UpdateIssueFieldRequest(
	String label,
	String description,
	FieldType fieldType,
	Boolean required,
	List<String> allowedOptions
) {
}
