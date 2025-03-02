package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDateTime;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.SubTask;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateSubTaskRequest(

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String title,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String content,

	@StandardText
	String summary,

	IssuePriority priority,
	LocalDateTime dueAt

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}

	@Override
	public void update(Issue issue) {
		SubTask subTask = (SubTask)issue;

		subTask.updateTitle(title);
		subTask.updateContent(content);
		subTask.updateSummary(summary);
		subTask.updatePriority(priority);
		subTask.updateDueAt(dueAt);
	}
}
