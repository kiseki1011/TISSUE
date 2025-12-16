package com.tissue.position.presentation.dto.request;

import com.tissue.common.validator.annotation.size.NameSize;
import com.tissue.common.validator.annotation.size.text.StandardText;

import jakarta.validation.constraints.NotBlank;

public record CreatePositionRequest(

	@NameSize
	@NotBlank(message = "{valid.notblank}")
	String name,

	@StandardText
	String description
) {
}
