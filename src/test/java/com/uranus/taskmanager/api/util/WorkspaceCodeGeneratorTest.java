package com.uranus.taskmanager.api.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkspaceCodeGeneratorTest {

	private WorkspaceCodeGenerator workspaceCodeGenerator;

	@BeforeEach
	void setUp() {
		workspaceCodeGenerator = new WorkspaceCodeGenerator();
	}

	@Test
	@DisplayName("워크스페이스 코드 생성기에 의해 8자리 코드가 생성되어야 한다")
	void testGenerateWorkspaceCodeLength() {

		String workspaceCode = workspaceCodeGenerator.generateWorkspaceCode();

		assertThat(workspaceCode).isNotNull();
		assertThat(workspaceCode.length()).isEqualTo(8);
	}

	@Test
	@DisplayName("워크스페이스 코드 생성기에 의해 생성된 코드는 Base62 형식이어야 한다")
	void testGenerateWorkspaceCodeFormat() {

		String workspaceCode = workspaceCodeGenerator.generateWorkspaceCode();

		assertThat(workspaceCode).matches("[A-Za-z0-9]+");
	}

}
