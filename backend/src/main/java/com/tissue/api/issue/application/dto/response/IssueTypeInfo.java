package com.tissue.api.issue.application.dto.response;

import com.tissue.api.common.enums.ColorType;

public record IssueTypeInfo(
	Long id,
	String displayName,
	ColorType color
	// String icon
) {
}
