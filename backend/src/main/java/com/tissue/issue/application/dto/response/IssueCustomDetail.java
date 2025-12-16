package com.tissue.issue.application.dto.response;

import java.util.List;

import com.tissue.issue.application.dto.response.info.CustomFieldValueInfo;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueFieldValue;

public record IssueCustomDetail(
	String issueKey,
	List<CustomFieldValueInfo> customFields
) {
	public static IssueCustomDetail from(
		Issue issue,
		List<IssueFieldValue> fieldValues
	) {
		return new IssueCustomDetail(
			issue.getKey(),
			fieldValues.stream()
				.map(CustomFieldValueInfo::from)
				.toList()
		);
	}
}
