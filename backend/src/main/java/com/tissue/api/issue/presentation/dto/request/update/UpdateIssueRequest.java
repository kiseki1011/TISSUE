package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
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
	// 모든 이슈 타입이 공통으로 가지는 필드들
	String title();

	String content();

	String summary();

	IssuePriority priority();

	LocalDate dueDate();

	// 이슈의 타입
	IssueType getType();

	// 실제 업데이트를 수행하는 메서드
	void update(Issue issue);
}
