package com.tissue.api.workspacemember.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.NicknamePattern;
import com.tissue.api.common.validator.annotation.size.NicknameSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateNicknameRequest(

	@NicknameSize
	@NicknamePattern
	@NotBlank(message = "{valid.notblank}")
	String nickname
) {
}
