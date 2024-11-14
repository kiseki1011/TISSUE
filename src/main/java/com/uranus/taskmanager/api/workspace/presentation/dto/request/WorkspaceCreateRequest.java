package com.uranus.taskmanager.api.workspace.presentation.dto.request;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class WorkspaceCreateRequest {

	@Size(min = 2, max = 50, message = "Workspace name must be 2 ~ 50 characters long")
	@NotBlank(message = "Workspace name must not be blank")
	private String name;

	@Size(min = 1, max = 255, message = "Workspace description must be 1 ~ 255 characters long")
	@NotBlank(message = "Workspace description must not be blank")
	private String description;

	@Pattern(regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric"
			+ " including at least one special character and must be between 8 and 30 characters")
	private String password;

	private String code;

	@Builder
	public WorkspaceCreateRequest(String name, String description, String password, String code) {
		this.name = name;
		this.description = description;
		this.password = password;
		this.code = code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Workspace to() {
		return Workspace.builder()
			.name(name)
			.description(description)
			.password(password)
			.code(code)
			.build();
	}

}
