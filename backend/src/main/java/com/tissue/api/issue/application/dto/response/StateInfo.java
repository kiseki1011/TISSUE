package com.tissue.api.issue.application.dto.response;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.domain.enums.StateCategory;

public record StateInfo(
	Long id,
	String displayName,
	StateCategory category,
	ColorType color
	// String icon
) {
}
