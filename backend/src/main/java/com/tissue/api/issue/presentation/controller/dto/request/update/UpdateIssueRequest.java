package com.tissue.api.issue.presentation.controller.dto.request.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.Issue;

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

	CommonIssueUpdateFields common();

	IssueType getType();

	void updateNonNullFields(Issue issue);
}
