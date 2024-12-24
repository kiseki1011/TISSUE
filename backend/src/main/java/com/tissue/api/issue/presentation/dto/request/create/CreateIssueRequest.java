package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.workspace.domain.Workspace;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXISTING_PROPERTY,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = CreateEpicRequest.class, name = "EPIC"),
	@JsonSubTypes.Type(value = CreateStoryRequest.class, name = "STORY"),
	@JsonSubTypes.Type(value = CreateTaskRequest.class, name = "TASK"),
	@JsonSubTypes.Type(value = CreateBugRequest.class, name = "BUG"),
	@JsonSubTypes.Type(value = CreateSubTaskRequest.class, name = "SUB_TASK")
})
public interface CreateIssueRequest {

	String title();

	String content();

	String summary();

	IssuePriority priority();

	LocalDate dueDate();

	Long parentIssueId();

	IssueType getType();

	Issue to(Workspace workspace, Issue parentIssue);
}
