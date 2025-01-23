package com.tissue.api.review.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.NotBlank;

public record UpdateReviewRequest(
	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String title,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String content
) {
}
