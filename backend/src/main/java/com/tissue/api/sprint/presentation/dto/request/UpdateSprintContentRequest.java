package com.tissue.api.sprint.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;

import lombok.Builder;

@Builder
public record UpdateSprintContentRequest(

	@ShortText
	String title,

	@LongText
	String goal
) {
}
