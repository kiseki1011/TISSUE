package com.tissue.api.issue.presentation.dto.request;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.lang.Nullable;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.issue.application.dto.CreateIssueCommand;
import com.tissue.api.issue.domain.enums.IssuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank @ContentText String content,
	@Nullable @LongText String summary,
	@NotNull IssuePriority priority,
	@Nullable LocalDateTime dueAt,
	@NotNull Long issueTypeId,
	Map<Long, Object> customFields
) {
	public CreateIssueCommand toCommand(String workspaceKey) {
		return CreateIssueCommand.builder()
			.workspaceKey(workspaceKey)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.issueTypeId(issueTypeId)
			.customFields(customFields == null ? Map.of() : customFields)
			.build();
	}
}
