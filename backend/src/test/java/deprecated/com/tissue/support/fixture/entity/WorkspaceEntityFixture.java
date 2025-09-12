package deprecated.com.tissue.support.fixture.entity;

import com.tissue.api.workspace.domain.model.Workspace;

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
