package com.tissue.api.team.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(

	@NameSize
	@NotBlank(message = "{valid.notblank}")
	String name,

	@StandardText
	@NotBlank(message = "{valid.notblank}")
	String description
) {
}
