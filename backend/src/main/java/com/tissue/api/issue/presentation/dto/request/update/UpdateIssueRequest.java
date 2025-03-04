package com.tissue.api.issue.presentation.dto.request.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXISTING_PROPERTY,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = UpdateEpicRequest.class, name = "EPIC"),
	@JsonSubTypes.Type(value = UpdateStoryRequest.class, name = "STORY"),
	@JsonSubTypes.Type(value = UpdateTaskRequest.class, name = "TASK"),
	@JsonSubTypes.Type(value = UpdateBugRequest.class, name = "BUG"),
	@JsonSubTypes.Type(value = UpdateSubTaskRequest.class, name = "SUB_TASK")
})
public interface UpdateIssueRequest {

	// String title();
	//
	// String content();
	//
	// String summary();
	//
	// IssuePriority priority();
	//
	// LocalDateTime dueAt();

	IssueType getType();

	void updateNonNullFields(Issue issue);
}
