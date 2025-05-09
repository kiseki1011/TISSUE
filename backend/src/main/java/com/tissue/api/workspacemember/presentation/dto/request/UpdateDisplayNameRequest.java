package com.tissue.api.workspacemember.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.DisplayNamePattern;
import com.tissue.api.common.validator.annotation.size.DisplayNameSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateDisplayNameRequest(

	@DisplayNameSize
	@DisplayNamePattern
	@NotBlank(message = "{valid.notblank}")
	String displayName
) {
}
