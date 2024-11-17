package com.uranus.taskmanager.api.workspace.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkspacePasswordUpdateRequest {

	private String originalPassword;

	@Pattern(
		regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric and must be between 4 and 30 characters"
	)
	private String updatePassword;

	@Builder
	public WorkspacePasswordUpdateRequest(String originalPassword, String updatePassword) {
		this.originalPassword = originalPassword;
		this.updatePassword = updatePassword;
	}
}
