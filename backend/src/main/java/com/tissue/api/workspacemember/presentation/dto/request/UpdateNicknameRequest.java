package com.tissue.api.workspacemember.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateNicknameRequest {

	@NotBlank(message = "Nickname must not be blank.")
	@Size(min = 2, max = 30, message = "Nickname must be between 2 and 30 characters.")
	private String nickname;

	public UpdateNicknameRequest(String nickname) {
		this.nickname = nickname;
	}
}
