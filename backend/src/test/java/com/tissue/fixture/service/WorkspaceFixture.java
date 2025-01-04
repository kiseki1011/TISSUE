package com.tissue.fixture.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspace.service.command.create.WorkspaceCreateService;

@Component
public class WorkspaceFixture {

	@Autowired
	private WorkspaceCreateService workspaceCreateService;

	public CreateWorkspaceResponse createWorkspace(Long memberId) {

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("Test Workspace")
			.description("Test Workspace")
			.build();

		return workspaceCreateService.createWorkspace(request, memberId);
	}
}
