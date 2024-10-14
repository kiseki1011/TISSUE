package com.uranus.taskmanager.api.workspace.dto.request;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder
public class WorkspaceCreateRequest {

	@Size(min = 2, max = 50, message = "Workspace name must be 2 ~ 50 characters long")
	@NotBlank(message = "Workspace name must not be blank")
	private final String name;

	@Size(min = 1, max = 255, message = "Workspace name must be 1 ~ 255 characters long")
	@NotBlank(message = "Workspace description must not be blank")
	private final String description;

	@Pattern(regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric"
			+ " including at least one special character and must be between 8 and 30 characters")
	private final String password;

	private String code;

	public void setCode(String code) {
		this.code = code;
	}

	public Workspace toEntity() {
		return Workspace.builder()
			.name(name)
			.description(description)
			.password(password)
			.code(code)
			.build();
	}

}
