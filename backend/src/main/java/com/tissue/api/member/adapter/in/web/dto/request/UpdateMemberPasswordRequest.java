package com.tissue.api.member.adapter.in.web.dto.request;

import com.tissue.api.common.validator.annotation.pattern.PasswordPattern;
import com.tissue.api.common.validator.annotation.size.password.PasswordSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberPasswordRequest(
	String originalPassword,

	@PasswordSize
	@PasswordPattern
	@NotBlank(message = "{valid.notblank}")
	String newPassword
) {
}
