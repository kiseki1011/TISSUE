package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateStoryRequest(

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String title,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String content,

	@StandardText
	String summary,

	IssuePriority priority,

	LocalDate dueDate,

	Difficulty difficulty,

	String parentIssueKey,

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
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueDate(dueDate)
			.difficulty(difficulty)
			.userStory(userStory)
			.acceptanceCriteria(acceptanceCriteria)
			.parentIssue(parentIssue)
			.build();
	}
}
