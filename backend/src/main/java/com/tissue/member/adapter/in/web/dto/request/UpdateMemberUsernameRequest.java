package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.pattern.UsernamePattern;
import com.tissue.common.validator.annotation.size.UsernameSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberUsernameRequest(
	@UsernamePattern
	@UsernameSize
	@NotBlank(message = "{valid.notblank}")
	String newUsername
) {
}
