package com.tissue.api.workspacemember.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
	@NotBlank(message = "Nickname must not be blank.")
	@Size(min = 2, max = 30, message = "Nickname must be between 2 and 30 characters.")
	String nickname
) {
}
