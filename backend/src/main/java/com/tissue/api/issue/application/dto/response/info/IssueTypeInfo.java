package com.tissue.api.issue.application.dto.response.info;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issuetype.domain.IssueType;

public record IssueTypeInfo(
	Long id,
	String displayName,
	ColorType color,
	// String icon
	boolean canUseStoryPoint
) {
	public static IssueTypeInfo from(IssueType issueType) {
		return new IssueTypeInfo(
			issueType.getId(),
			issueType.getDisplayLabel(),
			issueType.getColor(),
			issueType.canUseStoryPoint()
		);
	}
}
