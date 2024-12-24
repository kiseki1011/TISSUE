package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateWorkspaceRequest(
	@Size(min = 2, max = 50, message = "Workspace name must be 2 ~ 50 characters long")
	@NotBlank(message = "Workspace name must not be blank")
	String name,

	@Size(min = 1, max = 255, message = "Workspace description must be 1 ~ 255 characters long")
	@NotBlank(message = "Workspace description must not be blank")
	String description,

	@Pattern(regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric"
			+ " including at least one special character and must be between 8 and 30 characters")
	String password,
	
	@Pattern(regexp = "^[a-zA-Z]+$", message = "Must contain only alphabetic characters.")
	@Size(min = 3, max = 16, message = "A issue key must between 3 ~ 16 characters long.")
	String keyPrefix
) {

	public static Workspace to(CreateWorkspaceRequest request) {
		return Workspace.builder()
			.name(request.name)
			.description(request.description)
			.password(request.password)
			.build();
	}

}
