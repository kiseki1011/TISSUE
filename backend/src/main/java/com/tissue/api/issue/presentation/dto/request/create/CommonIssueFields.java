package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDateTime;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.enums.IssuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CommonIssueFields(

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
	LocalDateTime dueAt

) {
}
