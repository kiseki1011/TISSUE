package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.IssueKeyPrefixPattern;
import com.tissue.api.common.validator.annotation.pattern.SimplePasswordPattern;
import com.tissue.api.common.validator.annotation.size.IssueKeyPrefixSize;
import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.password.SimplePasswordSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateWorkspaceRequest(
	@NameSize
	@NotBlank(message = "{valid.notblank}")
	String name,

	@StandardText
	@NotBlank(message = "{valid.notblank}")
	String description,

	@SimplePasswordSize
	@SimplePasswordPattern
	String password,

	@IssueKeyPrefixSize
	@IssueKeyPrefixPattern
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
