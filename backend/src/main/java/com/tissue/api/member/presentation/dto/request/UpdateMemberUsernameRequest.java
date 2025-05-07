package com.tissue.api.member.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.UsernamePattern;
import com.tissue.api.common.validator.annotation.size.UsernameSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberUsernameRequest(
	@UsernamePattern
	@UsernameSize
	@NotBlank(message = "{valid.notblank}")
	String newUsername
) {
}
