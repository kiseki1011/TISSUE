package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDateTime;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Todo
 *  - add difficulty
 */
@Builder
public record CreateEpicRequest(

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String title,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String content,

	@StandardText
	String summary,

	IssuePriority priority,

	@NotNull(message = "{valid.notnull}")
	LocalDateTime dueAt,
	String parentIssueKey,

	@LongText
	@NotBlank(message = "{valid.notblank}")
	String businessGoal

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return Epic.builder()
			.workspace(workspace)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.businessGoal(businessGoal)
			.build();
	}
}
