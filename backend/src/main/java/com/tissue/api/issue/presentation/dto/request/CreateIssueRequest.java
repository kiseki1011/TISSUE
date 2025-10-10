package com.tissue.api.issue.presentation.dto.request;

import java.time.Instant;
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
	@Nullable @ContentText String content,
	@Nullable @LongText String summary,
	IssuePriority priority, // TODO: not-null vs nullable?
	@Nullable Instant dueAt,
	@NotNull Long issueTypeId,
	@Nullable Map<Long, Object> customFields
) {
	public CreateIssueCommand toCommand(String workspaceKey, Long currentMemberId) {
		return CreateIssueCommand.builder()
			.currentMemberId(currentMemberId)
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
