package com.uranus.taskmanager.fixture.dto;

import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;

public class WorkspaceCreateDtoFixture {

	public WorkspaceCreateRequest createWorkspaceCreateRequest(String password) {
		return WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.password(password)
			.build();
	}
}
