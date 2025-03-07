package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDateTime;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.enums.IssuePriority;

import lombok.Builder;

@Builder
public record CommonIssueUpdateFields(

	@ShortText
	String title,

	@ContentText
	String content,

	@StandardText
	String summary,

	IssuePriority priority,

	LocalDateTime dueAt
) {
}
