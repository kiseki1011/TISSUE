package com.tissue.integration.service.validator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceAuthenticationServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("입력 패스워드가 저장된 워크스페이스의 암호화된 패스워드와 일치하지 검증한다")
	void validateIfInputPasswordMatchesWorkspacePassword() {
		// given
		Workspace workspace = testDataFixture.createWorkspace("test workspace", "test1234!", null);

		// when & then
		assertThatThrownBy(() -> workspaceAuthenticationService.authenticate("invalidPassword", workspace.getKey()))
			.isInstanceOf(AuthenticationFailedException.class);
	}
}