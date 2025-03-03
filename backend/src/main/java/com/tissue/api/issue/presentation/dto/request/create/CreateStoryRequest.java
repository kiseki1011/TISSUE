package com.tissue.api.issue.presentation.dto.request.create;

import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateStoryRequest(

	@Valid
	CommonIssueFields common,

	@Min(value = 0, message = "{valid.storypoint.min}")
	@Max(value = 100, message = "{valid.storypoint.max}")
	Integer storyPoint,

	@LongText
	@NotBlank(message = "{valid.notblank}")
	String userStory,

	@LongText
	String acceptanceCriteria

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return Story.builder()
			.workspace(workspace)
			.title(common.title())
			.content(common.content())
			.summary(common.summary())
			.priority(common.priority())
			.dueAt(common.dueAt())
			.storyPoint(storyPoint)
			.userStory(userStory)
			.acceptanceCriteria(acceptanceCriteria)
			.parentIssue(parentIssue)
			.build();
	}
}
