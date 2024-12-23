package com.tissue.fixture.entity;

import com.tissue.api.workspace.domain.Workspace;

public class WorkspaceEntityFixture {

	public Workspace createWorkspace(String code) {
		return Workspace.builder()
			.code(code)
			.name("test name")
			.description("test description")
			.password("workspace1234!")
			.build();
	}
}
