package com.uranus.taskmanager.fixture.dto;

import com.uranus.taskmanager.api.workspace.presentation.dto.request.CreateWorkspaceRequest;

public class WorkspaceCreateDtoFixture {

	public CreateWorkspaceRequest createWorkspaceCreateRequest(String password) {
		return CreateWorkspaceRequest.builder()
			.name("test name")
			.description("test description")
			.password(password)
			.build();
	}
}
